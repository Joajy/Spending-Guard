import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { MonthlyBudgetView } from "./MonthlyBudgetView";

const replace = vi.fn();
const refresh = vi.fn();
const router = { replace, refresh };

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

const budget = {
  month: "2026-08",
  limitAmount: 1_000_000,
  spentAmount: 320_000,
  remainingAmount: 680_000,
  version: 2,
  updatedAt: "2026-08-28T10:00:00Z",
};

describe("MonthlyBudgetView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("shows the budget and saves a revision with its current version", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(budget), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({
        ...budget,
        limitAmount: 1_500_000,
        remainingAmount: 1_180_000,
        version: 3,
      }), { status: 200 }));
    const user = userEvent.setup();
    render(<MonthlyBudgetView initialMonth="2026-08" />);

    expect(await screen.findByText("680,000원")).toBeInTheDocument();
    const input = screen.getByRole("textbox", { name: "월간 예산" });
    await user.clear(input);
    await user.type(input, "1500000");
    await user.click(screen.getByRole("button", { name: "예산 수정" }));

    expect(await screen.findByRole("status")).toHaveTextContent("예산을 수정했습니다");
    expect(request).toHaveBeenLastCalledWith("/api/budget?month=2026-08", expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({ amount: 1_500_000, version: 2 }),
    }));
  });

  it("creates a first budget from an empty state", async () => {
    const created = { ...budget, spentAmount: 0, remainingAmount: 500_000, limitAmount: 500_000, version: 0 };
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "없음" }), { status: 404 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(created), { status: 200 }));
    const user = userEvent.setup();
    render(<MonthlyBudgetView initialMonth="2026-08" />);

    expect(await screen.findByText("아직 설정한 예산이 없습니다.")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "50만원" }));
    await user.click(screen.getByRole("button", { name: "예산 설정" }));

    expect(await screen.findByRole("status")).toHaveTextContent("예산을 설정했습니다");
    expect(request).toHaveBeenLastCalledWith("/api/budget?month=2026-08", expect.objectContaining({
      body: JSON.stringify({ amount: 500_000, version: null }),
    }));
  });

  it("asks for the latest value after a concurrent update conflict", async () => {
    const latest = { ...budget, limitAmount: 1_200_000, version: 3 };
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify(budget), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "conflict" }), { status: 409 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(latest), { status: 200 }));
    const user = userEvent.setup();
    render(<MonthlyBudgetView initialMonth="2026-08" />);

    const input = await screen.findByRole("textbox", { name: "월간 예산" });
    await user.clear(input);
    await user.type(input, "900000");
    await user.click(screen.getByRole("button", { name: "예산 수정" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("다른 화면에서 예산이 먼저 변경되었습니다");
    await user.click(screen.getByRole("button", { name: "최신 예산 불러오기" }));
    await waitFor(() => expect(screen.getByRole("textbox", { name: "월간 예산" })).toHaveValue("1,200,000"));
    expect(request).toHaveBeenCalledTimes(3);
  });

  it("validates the amount, changes month and retries a failed load", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "잠시 후 다시 시도" }), { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(budget), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(budget), { status: 200 }));
    const user = userEvent.setup();
    render(<MonthlyBudgetView initialMonth="2026-01" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("잠시 후 다시 시도");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(await screen.findByText("680,000원")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "이전 달" }));
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      "/api/budget?month=2025-12",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    ));
  });

  it("redirects when the session has expired", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));
    render(<MonthlyBudgetView initialMonth="2026-08" />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });
});
