import { act, fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TransactionHistoryView } from "./TransactionHistoryView";
import type { SpendEventHistoryItem, SpendEventHistoryPage } from "./types";

const replace = vi.fn();
const refresh = vi.fn();
const router = { replace, refresh };

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

const payment: SpendEventHistoryItem = {
  eventId: "11111111-1111-1111-1111-111111111111",
  displayText: "쿠팡 12,800원 결제",
  status: "ANALYZING",
  transactionAt: "2026-08-27T00:30:00Z",
  amount: 12_800,
  currency: "KRW",
  transactionType: "PAYMENT",
  category: "SHOPPING",
  fixedCost: false,
  riskLevel: "MEDIUM",
  categoryVersion: 0,
};

const refund: SpendEventHistoryItem = {
  ...payment,
  eventId: "22222222-2222-2222-2222-222222222222",
  displayText: "쿠팡 12,800원 환불",
  transactionType: "REFUND",
  riskLevel: "LOW",
};

const stalePageItem: SpendEventHistoryItem = {
  ...payment,
  eventId: "33333333-3333-3333-3333-333333333333",
  displayText: "이전 필터의 다음 페이지",
};

function page(
  items: SpendEventHistoryItem[],
  nextCursor: string | null = null,
): SpendEventHistoryPage {
  return { items, nextCursor, hasNext: nextCursor !== null };
}

describe("TransactionHistoryView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("renders normalized transaction information", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page([payment])), { status: 200 }),
    );

    render(<TransactionHistoryView initialMonth="2026-08" />);

    expect(await screen.findByText("쿠팡 12,800원 결제")).toBeInTheDocument();
    const history = within(screen.getByRole("region", { name: "소비 내역 목록" }));
    expect(history.getByText("쇼핑")).toBeInTheDocument();
    expect(history.getByText("-12,800원")).toBeInTheDocument();
    expect(history.getByText("주의")).toBeInTheDocument();
  });

  it("reloads when status and category filters change", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page([payment])), { status: 200 }),
    );
    const user = userEvent.setup();
    render(<TransactionHistoryView initialMonth="2026-08" />);
    await screen.findByText("쿠팡 12,800원 결제");

    await user.selectOptions(screen.getByLabelText("처리 상태"), "NEEDS_REVIEW");
    await user.selectOptions(screen.getByLabelText("카테고리"), "DELIVERY");

    await waitFor(() => expect(request).toHaveBeenLastCalledWith(
      "/api/transactions?month=2026-08&status=NEEDS_REVIEW&category=DELIVERY",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    ));
  });

  it("appends the next cursor page without replacing current items", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([payment], "next-page")), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(page([refund])), { status: 200 }));
    const user = userEvent.setup();
    render(<TransactionHistoryView initialMonth="2026-08" />);
    await screen.findByText("쿠팡 12,800원 결제");

    await user.click(screen.getByRole("button", { name: "소비 내역 더 보기" }));

    expect(await screen.findByText("쿠팡 12,800원 환불")).toBeInTheDocument();
    expect(screen.getByText("쿠팡 12,800원 결제")).toBeInTheDocument();
    expect(globalThis.fetch).toHaveBeenLastCalledWith(
      "/api/transactions?month=2026-08&cursor=next-page",
    );
  });

  it("ignores a late next-page response after filters change", async () => {
    let resolveStalePage!: (response: Response) => void;
    const stalePage = new Promise<Response>((resolve) => {
      resolveStalePage = resolve;
    });
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(page([payment], "next-page")), { status: 200 }))
      .mockReturnValueOnce(stalePage)
      .mockResolvedValueOnce(new Response(JSON.stringify(page([refund])), { status: 200 }));
    const user = userEvent.setup();
    render(<TransactionHistoryView initialMonth="2026-08" />);
    await screen.findByText("쿠팡 12,800원 결제");

    await user.click(screen.getByRole("button", { name: "소비 내역 더 보기" }));
    await user.selectOptions(screen.getByLabelText("카테고리"), "SHOPPING");
    expect(await screen.findByText("쿠팡 12,800원 환불")).toBeInTheDocument();

    await act(async () => {
      resolveStalePage(new Response(JSON.stringify(page([stalePageItem])), { status: 200 }));
      await stalePage;
    });
    expect(screen.queryByText("이전 필터의 다음 페이지")).not.toBeInTheDocument();
  });

  it("redirects to login when the session is no longer valid", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));

    render(<TransactionHistoryView initialMonth="2026-08" />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("offers a retry after a temporary loading failure", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "잠시 후 다시 시도" }), { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(page([payment])), { status: 200 }));
    render(<TransactionHistoryView initialMonth="2026-08" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("잠시 후 다시 시도");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));

    expect(await screen.findByText("쿠팡 12,800원 결제")).toBeInTheDocument();
    expect(globalThis.fetch).toHaveBeenCalledTimes(2);
  });
});
