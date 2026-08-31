import { describe, expect, it } from "vitest";
import { formatBudgetInput, parseBudgetAmount } from "./format";

describe("budget amount format", () => {
  it("keeps digits and adds thousands separators", () => {
    expect(formatBudgetInput("₩ 001250000")).toBe("1,250,000");
  });

  it("returns an empty value when no digit was entered", () => {
    expect(formatBudgetInput("원")).toBe("");
  });

  it("parses a valid budget within the service range", () => {
    expect(parseBudgetAmount("1,250,000")).toBe(1_250_000);
  });

  it("rejects zero and values above one trillion won", () => {
    expect(parseBudgetAmount("0")).toBeNull();
    expect(parseBudgetAmount("1,000,000,000,001")).toBeNull();
  });
});
