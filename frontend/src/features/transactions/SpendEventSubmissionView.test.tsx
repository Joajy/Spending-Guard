import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SpendEventSubmissionView } from "./SpendEventSubmissionView";

const replace = vi.fn();
const refresh = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, refresh }),
  usePathname: () => "/transactions/new",
}));

const receipt = {
  eventId: "a0e6cfcc-7d18-43f1-ae90-a60eb83b005c",
  status: "RECEIVED",
  receivedAt: "2026-08-31T13:00:00Z",
};
const analyzed = {
  eventId: receipt.eventId,
  source: "MANUAL_TEXT",
  status: "COMPLETED",
  occurredAt: "2026-08-31T12:30:00Z",
  receivedAt: receipt.receivedAt,
  fastParse: {
    amount: 12800,
    currency: "KRW",
    transactionType: "PAYMENT",
    status: "PARSED",
    reviewReason: null,
    category: "SHOPPING",
    fixedCost: false,
    riskLevel: "LOW",
    riskReason: "일반 결제",
    parserVersion: "fast-v1",
    parsedAt: "2026-08-31T13:00:01Z",
    categoryVersion: 0,
  },
};

describe("SpendEventSubmissionView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("submits an alert and shows the parsed result", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(receipt), { status: 202 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(analyzed), { status: 200 }));
    const user = userEvent.setup();
    render(<SpendEventSubmissionView />);

    await user.type(screen.getByLabelText("금융 알림 텍스트"), "쿠팡 12,800원 결제");
    await user.click(screen.getByRole("button", { name: "분석 요청하기" }));

    expect(await screen.findByText("12,800원")).toBeVisible();
    expect(screen.getByText("SHOPPING")).toBeVisible();
    expect(screen.getByText("분석 완료")).toBeVisible();
    expect(request).toHaveBeenNthCalledWith(1, "/api/transactions", expect.objectContaining({ method: "POST" }));
    expect(request).toHaveBeenNthCalledWith(2, `/api/transactions/${receipt.eventId}`, { cache: "no-store" });
  });

  it("rejects blank content before making a request", async () => {
    const request = vi.spyOn(globalThis, "fetch");
    render(<SpendEventSubmissionView />);
    fireEvent.submit(screen.getByRole("button", { name: "분석 요청하기" }).closest("form")!);
    expect(await screen.findByRole("alert")).toHaveTextContent("1~2,000자");
    expect(request).not.toHaveBeenCalled();
  });

  it("keeps an accepted event visible when status lookup fails", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(receipt), { status: 202 }))
      .mockRejectedValueOnce(new Error("offline"));
    render(<SpendEventSubmissionView />);
    fireEvent.change(screen.getByLabelText("금융 알림 텍스트"), { target: { value: "택시 20,000원" } });
    fireEvent.submit(screen.getByRole("button", { name: "분석 요청하기" }).closest("form")!);
    expect(await screen.findByText("안전하게 접수됨")).toBeVisible();
    expect(screen.getByText(/접수된 알림은 보관/)).toBeVisible();
  });

  it("shows a submission failure without presenting it as accepted", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "일시적으로 접수할 수 없습니다." }), { status: 503 }),
    );
    render(<SpendEventSubmissionView />);
    fireEvent.change(screen.getByLabelText("금융 알림 텍스트"), { target: { value: "배달 18,000원" } });
    fireEvent.submit(screen.getByRole("button", { name: "분석 요청하기" }).closest("form")!);
    expect(await screen.findByRole("alert")).toHaveTextContent("일시적으로");
    expect(screen.queryByText("안전하게 접수됨")).not.toBeInTheDocument();
  });

  it("moves to login when the session expires", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));
    render(<SpendEventSubmissionView />);
    fireEvent.change(screen.getByLabelText("금융 알림 텍스트"), { target: { value: "배달 18,000원" } });
    fireEvent.submit(screen.getByRole("button", { name: "분석 요청하기" }).closest("form")!);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(refresh).toHaveBeenCalled();
  });
});
