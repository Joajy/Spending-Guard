import { afterEach, describe, expect, it } from "vitest";
import { secureCookiesEnabled } from "./cookies";

const original = process.env.SESSION_COOKIE_SECURE;

afterEach(() => {
  if (original === undefined) delete process.env.SESSION_COOKIE_SECURE;
  else process.env.SESSION_COOKIE_SECURE = original;
});

describe("secureCookiesEnabled", () => {
  it("uses an explicit local HTTP override", () => {
    process.env.SESSION_COOKIE_SECURE = "false";
    expect(secureCookiesEnabled()).toBe(false);
  });

  it("enables secure cookies when explicitly requested", () => {
    process.env.SESSION_COOKIE_SECURE = "true";
    expect(secureCookiesEnabled()).toBe(true);
  });
});
