import { describe, expect, it } from "vitest";
import { currentServiceMonth } from "./serviceTime";

describe("currentServiceMonth", () => {
  it("uses Asia/Seoul when UTC and the service month differ", () => {
    expect(currentServiceMonth(new Date("2026-08-31T15:30:00Z"))).toBe("2026-09");
  });
});
