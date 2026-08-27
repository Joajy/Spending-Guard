import { describe, expect, it } from "vitest";
import { budgetProgress, formatWon, monthLabel, moveMonth } from "./format";

describe("dashboard formatting", () => {
  it("formats won amounts and Korean month labels", () => {
    expect(formatWon(12800)).toBe("12,800원");
    expect(monthLabel("2026-08")).toBe("2026년 8월");
  });

  it("caps budget usage and handles an unset budget", () => {
    expect(budgetProgress(30_000, 100_000)).toBe(30);
    expect(budgetProgress(120_000, 100_000)).toBe(100);
    expect(budgetProgress(10_000, 0)).toBe(0);
  });

  it("moves across year boundaries", () => {
    expect(moveMonth("2026-01", -1)).toBe("2025-12");
    expect(moveMonth("2026-12", 1)).toBe("2027-01");
  });
});
