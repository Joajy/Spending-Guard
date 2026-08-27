import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardView } from "./DashboardView";
import type { MonthlyDashboard } from "./types";

const replace = vi.fn();
const refresh = vi.fn();
const router = { replace, refresh };

vi.mock("next/navigation", () => ({
  useRouter: () => router,
}));

const dashboard: MonthlyDashboard = {
  month: "2026-08",
  budget: { limitAmount: 1_000_000, spentAmount: 320_000, remainingAmount: 680_000 },
  totalSpending: 320_000,
  transactionCount: 12,
  categories: [
    { category: "식비", amount: 180_000, transactionCount: 7 },
    { category: "교통", amount: 80_000, transactionCount: 5 },
  ],
  risks: [
    { riskLevel: "HIGH", count: 1 },
    { riskLevel: "MEDIUM", count: 2 },
    { riskLevel: "LOW", count: 9 },
  ],
};

describe("DashboardView", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("renders budget, category and risk summaries", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(dashboard), { status: 200 }),
    );
    render(<DashboardView initialMonth="2026-08" />);

    expect(await screen.findByText("680,000원")).toBeInTheDocument();
    expect(screen.getByText("식비")).toBeInTheDocument();
    expect(screen.getByText("주의가 필요한 소비").nextElementSibling).toHaveTextContent("3건");
    expect(screen.getByLabelText("예산 32% 사용")).toBeInTheDocument();
  });

  it("reloads the previous month and logs out", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(dashboard), { status: 200 }),
    );
    const user = userEvent.setup();
    render(<DashboardView initialMonth="2026-01" />);
    await screen.findByText("680,000원");

    await user.click(screen.getByRole("button", { name: "이전 달" }));
    await waitFor(() => expect(request).toHaveBeenCalledWith(
      "/api/dashboard?month=2025-12",
      expect.objectContaining({ signal: expect.any(AbortSignal) }),
    ));

    request.mockResolvedValue(new Response(null, { status: 204 }));
    await user.click(screen.getByRole("button", { name: "로그아웃" }));
    expect(request).toHaveBeenCalledWith("/api/session", { method: "DELETE" });
    expect(replace).toHaveBeenCalledWith("/login");
  });

  it("redirects when the session has expired", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));
    render(<DashboardView initialMonth="2026-08" />);
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
  });

  it("offers a retry when loading fails", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ message: "잠시 후 다시 시도" }), { status: 503 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(dashboard), { status: 200 }));
    render(<DashboardView initialMonth="2026-08" />);

    expect(await screen.findByRole("alert")).toHaveTextContent("잠시 후 다시 시도");
    fireEvent.click(screen.getByRole("button", { name: "다시 시도" }));
    expect(await screen.findByText("680,000원")).toBeInTheDocument();
    expect(request).toHaveBeenCalledTimes(2);
  });
});
