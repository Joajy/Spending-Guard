import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TossConfirmationView } from "./TossConfirmationView";

vi.mock("@/features/navigation/AppHeader", () => ({ AppHeader: () => <header /> }));

describe("TossConfirmationView", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("confirms the payment and then demonstrates full cancellation", async () => {
    const user = userEvent.setup();
    const backend = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ status: "DONE" }), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ status: "CANCELED" }), { status: 200 }));

    render(<TossConfirmationView paymentKey="payment-key" orderId="sg_order123" amount="12800" />);

    expect(await screen.findByText("자동 수집을 시작했습니다")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "전체 취소도 시연하기" }));
    expect(await screen.findByText(/취소 내역과 예산 감소/)).toBeInTheDocument();
    expect(backend).toHaveBeenNthCalledWith(
      2,
      "/api/toss-test/orders/sg_order123/cancel",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("rejects malformed redirect parameters before calling the backend", async () => {
    const backend = vi.spyOn(globalThis, "fetch");

    render(<TossConfirmationView paymentKey="" orderId="" amount="not-a-number" />);

    expect(await screen.findByText("승인을 확인해 주세요")).toBeInTheDocument();
    expect(backend).not.toHaveBeenCalled();
  });

  it("shows a confirmation error returned by the server", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(
      JSON.stringify({ message: "저장된 주문 금액과 다릅니다." }),
      { status: 409 },
    ));

    render(<TossConfirmationView paymentKey="payment-key" orderId="sg_order123" amount="12800" />);

    await waitFor(() => expect(screen.getByText("저장된 주문 금액과 다릅니다.")).toBeInTheDocument());
    expect(screen.getByText("승인을 확인해 주세요")).toBeInTheDocument();
  });
});
