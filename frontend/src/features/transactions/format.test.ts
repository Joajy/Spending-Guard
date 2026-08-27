import { describe, expect, it } from "vitest";
import { formatFullDateTime, formatTransactionAmount, formatTransactionTime } from "./format";

describe("transaction format", () => {
  it("uses opposite signs for payments and refunds", () => {
    expect(formatTransactionAmount(12_800, "PAYMENT")).toBe("-12,800원");
    expect(formatTransactionAmount(12_800, "REFUND")).toBe("+12,800원");
  });

  it("shows a pending label when the amount is not confirmed", () => {
    expect(formatTransactionAmount(null, null)).toBe("금액 확인 중");
  });

  it("formats transaction time in the service timezone", () => {
    expect(formatTransactionTime("2026-08-27T00:30:00Z")).toContain("09:30");
    expect(formatTransactionTime(null)).toBe("거래 시각 확인 중");
    expect(formatFullDateTime("invalid")).toBe("시각 확인 중");
  });
});
