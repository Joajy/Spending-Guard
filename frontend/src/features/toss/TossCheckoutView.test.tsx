import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TossCheckoutView } from "./TossCheckoutView";

const replace = vi.fn();
const refresh = vi.fn();
const requestPayment = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, refresh }),
}));
vi.mock("next/script", () => ({
  default: ({ onLoad }: { onLoad?: () => void }) => (
    <button data-testid="toss-sdk-script" type="button" onClick={onLoad} />
  ),
}));
vi.mock("@/features/navigation/AppHeader", () => ({ AppHeader: () => <header /> }));

describe("TossCheckoutView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
    requestPayment.mockReset().mockResolvedValue(undefined);
    Object.assign(window, {
      TossPayments: vi.fn(() => ({
        payment: vi.fn(() => ({ requestPayment })),
      })),
    });
  });

  it("creates a server order before requesting Toss payment", async () => {
    const user = userEvent.setup();
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      orderId: "sg_order123",
      clientKey: "test_ck",
      amount: 12_800,
      orderName: "Spending Guard 자동수집 테스트",
    }), { status: 200 }));
    render(<TossCheckoutView />);
    fireEvent.click(screen.getByTestId("toss-sdk-script"));

    await user.click(screen.getByRole("button", { name: "Toss 테스트 결제하기" }));

    await waitFor(() => expect(requestPayment).toHaveBeenCalledWith(expect.objectContaining({
      method: "CARD",
      orderId: "sg_order123",
      amount: { currency: "KRW", value: 12_800 },
    })));
    expect(fetch).toHaveBeenCalledWith("/api/toss-test/orders", expect.objectContaining({
      method: "POST",
    }));
  });

  it("shows the backend error without opening the payment window", async () => {
    const user = userEvent.setup();
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(
      JSON.stringify({ message: "Toss 연동이 비활성화되어 있습니다." }),
      { status: 503 },
    ));
    render(<TossCheckoutView />);
    fireEvent.click(screen.getByTestId("toss-sdk-script"));

    await user.click(screen.getByRole("button", { name: "Toss 테스트 결제하기" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("비활성화");
    expect(requestPayment).not.toHaveBeenCalled();
  });
});
