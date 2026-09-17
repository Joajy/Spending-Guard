import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RiskAlertFeed } from "./RiskAlertFeed";
import type { RiskAlertItem, RiskAlertPage } from "./types";

const replace = vi.fn();
const refresh = vi.fn();
const router = { replace, refresh };

vi.mock("next/navigation", () => ({
  useRouter: () => router,
  usePathname: () => "/alerts",
}));

const highAlert: RiskAlertItem = {
  eventId: "11111111-1111-1111-1111-111111111111",
  displayText: "카카오T 28,000원 결제",
  transactionAt: "2026-08-27T17:10:00Z",
  amount: 28_000,
  category: "TRANSPORT",
  riskLevel: "HIGH",
  reasonCode: "LATE_NIGHT_TRANSPORT",
  categoryVersion: 0,
};

const mediumAlert: RiskAlertItem = {
  ...highAlert,
  eventId: "22222222-2222-2222-2222-222222222222",
  displayText: "배달 32,000원 결제",
  amount: 32_000,
  category: "DELIVERY",
  riskLevel: "MEDIUM",
  reasonCode: "HIGH_DELIVERY_AMOUNT",
};

function page(items: RiskAlertItem[], nextCursor: string | null = null): RiskAlertPage {
  return { items, nextCursor, hasNext: nextCursor !== null };
}

const detail = {
  eventId: highAlert.eventId,
  source: "MANUAL_TEXT",
  status: "COMPLETED",
  occurredAt: highAlert.transactionAt,
  receivedAt: "2026-08-27T17:10:01Z",
  fastParse: {
    amount: 28_000,
    currency: "KRW",
    transactionType: "PAYMENT",
    status: "PARSED",
    reviewReason: null,
    category: "TRANSPORT",
    fixedCost: false,
    riskLevel: "HIGH",
    riskReason: "LATE_NIGHT_TRANSPORT",
    parserVersion: "fast-v1",
    parsedAt: "2026-08-27T17:10:02Z",
    categoryVersion: 0,
  },
};

describe("RiskAlertFeed", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("shows severity, reason and normalized transaction information", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page([highAlert, mediumAlert])), { status: 200 }),
    );

    render(<RiskAlertFeed initialMonth="2026-08" />);

    const feed = within(await screen.findByRole("region", { name: "위험 알림 목록" }));
    expect(feed.getByText("높음")).toBeInTheDocument();
    expect(feed.getByText("주의")).toBeInTheDocument();
    expect(feed.getByText("심야 시간대의 교통비 결제입니다.")).toBeInTheDocument();
    expect(feed.getByText("28,000원")).toBeInTheDocument();
  });

  it("reloads the feed when the minimum risk level changes", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page([highAlert])), { status: 200 }),
    );
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "높음만" }));

    await waitFor(() => expect(request).toHaveBeenLastCalledWith(
      "/api/risk-alerts?month=2026-08&minimumLevel=HIGH",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    ));
    expect(screen.getByRole("button", { name: "높음만" })).toHaveAttribute("aria-pressed", "true");
  });

  it("moves to the previous month without keeping the old result", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page([highAlert])), { status: 200 }),
    );
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "이전 달" }));

    await waitFor(() => expect(request).toHaveBeenLastCalledWith(
      "/api/risk-alerts?month=2026-07&minimumLevel=MEDIUM",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    ));
    expect(screen.getByText("2026년 7월")).toBeInTheDocument();
  });

  it("appends the next cursor page", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert], "next-page")), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(page([mediumAlert])), { status: 200 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "위험 알림 더 보기" }));

    expect(await screen.findByText("배달 32,000원 결제")).toBeInTheDocument();
    expect(screen.getByText("카카오T 28,000원 결제")).toBeInTheDocument();
    expect(globalThis.fetch).toHaveBeenLastCalledWith(
      "/api/risk-alerts?month=2026-08&minimumLevel=MEDIUM&cursor=next-page",
    );
  });

  it("keeps the current alerts when the next page fails", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert], "next-page")), { status: 200 }))
      .mockRejectedValueOnce(new Error("offline"));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "위험 알림 더 보기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("네트워크 연결");
    expect(screen.getByText("카카오T 28,000원 결제")).toBeInTheDocument();
  });

  it("ignores a late next-page response after the level changes", async () => {
    let resolveStalePage!: (response: Response) => void;
    const stalePage = new Promise<Response>((resolve) => {
      resolveStalePage = resolve;
    });
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert], "next-page")), { status: 200 }))
      .mockReturnValueOnce(stalePage)
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "위험 알림 더 보기" }));
    await user.click(screen.getByRole("button", { name: "높음만" }));
    await waitFor(() => expect(globalThis.fetch).toHaveBeenCalledTimes(3));

    await act(async () => {
      resolveStalePage(new Response(JSON.stringify(page([mediumAlert])), { status: 200 }));
      await stalePage;
    });
    expect(screen.queryByText("배달 32,000원 결제")).not.toBeInTheDocument();
  });

  it("opens the original transaction and returns focus after closing", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(detail), { status: 200 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    const alert = await screen.findByRole("button", { name: "카카오T 28,000원 결제 상세 보기" });

    await user.click(alert);
    expect(await screen.findByText("직접 입력")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "상세 닫기" }));

    await waitFor(() => expect(alert).toHaveFocus());
  });

  it("removes an alert when category correction lowers its risk", async () => {
    const correction = {
      eventId: highAlert.eventId,
      originalCategory: "TRANSPORT",
      category: "OTHER",
      fixedCost: false,
      riskLevel: "LOW",
      riskReason: "NORMAL_PATTERN",
      version: 1,
      correctedAt: "2026-08-27T17:11:00Z",
    };
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(detail), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(correction), { status: 200 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);

    await user.click(await screen.findByRole("button", { name: "카카오T 28,000원 결제 상세 보기" }));
    const panel = within(await screen.findByRole("dialog"));
    await user.selectOptions(panel.getByLabelText("카테고리"), "OTHER");
    await user.click(panel.getByRole("button", { name: "카테고리 저장" }));

    expect(await screen.findByRole("status")).toHaveTextContent("현재 알림 목록에서 제외되었습니다.");
    expect(screen.queryByText("카카오T 28,000원 결제")).not.toBeInTheDocument();
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("updates a visible alert after category correction", async () => {
    const correction = {
      eventId: highAlert.eventId,
      originalCategory: "TRANSPORT",
      category: "DELIVERY",
      fixedCost: false,
      riskLevel: "MEDIUM",
      riskReason: "HIGH_DELIVERY_AMOUNT",
      version: 1,
      correctedAt: "2026-08-27T17:11:00Z",
    };
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(detail), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(correction), { status: 200 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);

    await user.click(await screen.findByRole("button", { name: "카카오T 28,000원 결제 상세 보기" }));
    const panel = within(await screen.findByRole("dialog"));
    await user.selectOptions(panel.getByLabelText("카테고리"), "DELIVERY");
    await user.click(panel.getByRole("button", { name: "카테고리 저장" }));
    expect(await screen.findByText("수정한 카테고리와 다시 계산된 위험도를 반영했습니다.")).toBeInTheDocument();
    await user.click(panel.getByRole("button", { name: "상세 닫기" }));

    const feed = within(screen.getByRole("region", { name: "위험 알림 목록" }));
    expect(feed.getByText(/^배달 ·/)).toBeInTheDocument();
    expect(feed.getByText("주의")).toBeInTheDocument();
    expect(feed.getByText("배달 카테고리의 비교적 큰 결제입니다.")).toBeInTheDocument();
  });

  it("redirects to login when the session is no longer valid", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));

    render(<RiskAlertFeed initialMonth="2026-08" />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("clears the session when the user logs out", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    render(<RiskAlertFeed initialMonth="2026-08" />);
    await screen.findByText("카카오T 28,000원 결제");

    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    expect(request).toHaveBeenLastCalledWith("/api/session", { method: "DELETE" });
    expect(replace).toHaveBeenCalledWith("/login");
    expect(refresh).toHaveBeenCalled();
  });

  it("offers a retry after a temporary loading failure", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "잠시 후 다시 시도" }), { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(page([highAlert])), { status: 200 }));
    render(<RiskAlertFeed initialMonth="2026-08" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("잠시 후 다시 시도");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));

    expect(await screen.findByText("카카오T 28,000원 결제")).toBeInTheDocument();
    expect(globalThis.fetch).toHaveBeenCalledTimes(2);
  });
});
