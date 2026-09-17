import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TransactionDetailPanel } from "./TransactionDetailPanel";
import type {
  CategoryCorrectionResult,
  SpendEventDetail,
  SpendEventHistoryItem,
} from "./types";

const replace = vi.fn();
const router = { replace };
vi.mock("next/navigation", () => ({ useRouter: () => router }));

const item: SpendEventHistoryItem = {
  eventId: "11111111-1111-4111-8111-111111111111",
  displayText: "쿠팡 12,800원 결제",
  status: "COMPLETED",
  transactionAt: "2026-08-27T00:30:00Z",
  amount: 12_800,
  currency: "KRW",
  transactionType: "PAYMENT",
  category: "SHOPPING",
  fixedCost: false,
  riskLevel: "MEDIUM",
  categoryVersion: 1,
};

const detail: SpendEventDetail = {
  eventId: item.eventId,
  source: "MANUAL_TEXT",
  status: "COMPLETED",
  occurredAt: "2026-08-27T00:30:00Z",
  receivedAt: "2026-08-27T00:30:01Z",
  fastParse: {
    amount: 12_800,
    currency: "KRW",
    transactionType: "PAYMENT",
    status: "PARSED",
    reviewReason: null,
    category: "SHOPPING",
    fixedCost: false,
    riskLevel: "MEDIUM",
    riskReason: "ELEVATED_AMOUNT",
    parserVersion: "fast-v1",
    parsedAt: "2026-08-27T00:30:02Z",
    categoryVersion: 1,
  },
};

const correction: CategoryCorrectionResult = {
  eventId: item.eventId,
  originalCategory: "SHOPPING",
  category: "DELIVERY",
  fixedCost: false,
  riskLevel: "LOW",
  riskReason: "NORMAL_PATTERN",
  version: 2,
  correctedAt: "2026-08-27T00:31:00Z",
};

describe("TransactionDetailPanel", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
  });

  it("shows the transaction source and risk reason", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(detail), { status: 200 }),
    );

    render(<TransactionDetailPanel item={item} onClose={vi.fn()} onCorrected={vi.fn()} />);

    expect(await screen.findByText("직접 입력")).toBeInTheDocument();
    expect(screen.getByText("분석 완료")).toBeInTheDocument();
    expect(screen.getByText("주의해서 확인할 금액대의 결제입니다.")).toBeInTheDocument();
    expect(screen.getByText("분석 기준 fast-v1")).toBeInTheDocument();
  });

  it("sends the viewed version and reflects the correction", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(detail), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(correction), { status: 200 }));
    const corrected = vi.fn();
    const user = userEvent.setup();
    render(<TransactionDetailPanel item={item} onClose={vi.fn()} onCorrected={corrected} />);
    await screen.findByText("직접 입력");

    await user.selectOptions(screen.getByLabelText("카테고리"), "DELIVERY");
    await user.click(screen.getByRole("button", { name: "카테고리 저장" }));

    expect(await screen.findByText("배달 카테고리로 수정했습니다.")).toBeInTheDocument();
    expect(request).toHaveBeenLastCalledWith(`/api/transactions/${item.eventId}`, {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ category: "DELIVERY", expectedVersion: 1 }),
    });
    expect(corrected).toHaveBeenCalledWith(correction);
    expect(screen.getByText("안정")).toBeInTheDocument();
  });

  it("reloads current data after a version conflict", async () => {
    const latest = {
      ...detail,
      fastParse: { ...detail.fastParse!, category: "TRANSPORT" as const, categoryVersion: 2 },
    };
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(detail), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "최신 내역을 다시 조회해 주세요." }), { status: 409 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(latest), { status: 200 }));
    const user = userEvent.setup();
    render(<TransactionDetailPanel item={item} onClose={vi.fn()} onCorrected={vi.fn()} />);
    await screen.findByText("직접 입력");

    await user.selectOptions(screen.getByLabelText("카테고리"), "DELIVERY");
    await user.click(screen.getByRole("button", { name: "카테고리 저장" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("최신 내역을 다시 조회해 주세요.");
    await user.click(screen.getByRole("button", { name: "최신 정보 불러오기" }));

    await waitFor(() => expect(request).toHaveBeenCalledTimes(3));
    expect(screen.getByLabelText("카테고리")).toHaveValue("TRANSPORT");
  });

  it("explains missing fields and prevents editing an unparsed transaction", async () => {
    const needsReview: SpendEventDetail = {
      ...detail,
      status: "NEEDS_REVIEW",
      fastParse: {
        ...detail.fastParse!,
        amount: null,
        transactionType: null,
        status: "NEEDS_REVIEW",
        reviewReason: "AMOUNT_NOT_FOUND,TRANSACTION_TYPE_NOT_FOUND",
        category: null,
        fixedCost: null,
        riskLevel: null,
        riskReason: null,
      },
    };
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(needsReview), { status: 200 }),
    );

    render(<TransactionDetailPanel item={item} onClose={vi.fn()} onCorrected={vi.fn()} />);

    expect(await screen.findByText(/결제 금액을 찾지 못했습니다/)).toHaveTextContent(
      "결제·취소·환불 여부를 판단하지 못했습니다.",
    );
    expect(screen.getByText("분석이 완료된 내역만 수정할 수 있습니다.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "카테고리 저장" })).toBeDisabled();
  });

  it("moves to login when the detail session has expired", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));

    render(<TransactionDetailPanel item={item} onClose={vi.fn()} onCorrected={vi.fn()} />);

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });
});
