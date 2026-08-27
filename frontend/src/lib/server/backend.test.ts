import { NextResponse } from "next/server";
import { afterEach, describe, expect, it } from "vitest";
import {
  backendUrl,
  clearSessionCookies,
  problemMessage,
  SESSION_COOKIES,
  setSessionCookies,
} from "./backend";

const tokens = {
  tokenType: "Bearer",
  accessToken: "access-token",
  accessTokenExpiresAt: "2026-08-27T00:15:00Z",
  refreshToken: "refresh-token",
  refreshTokenExpiresAt: "2026-09-10T00:00:00Z",
  userId: "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c",
};

describe("backend session helpers", () => {
  afterEach(() => delete process.env.BACKEND_API_URL);

  it("builds backend URLs without duplicate slashes", () => {
    process.env.BACKEND_API_URL = "http://backend:8080/";
    expect(backendUrl("/api/v1/status")).toBe("http://backend:8080/api/v1/status");
  });

  it("writes and clears HttpOnly session cookies", () => {
    const response = NextResponse.json({ ok: true });
    setSessionCookies(response, tokens, Date.parse("2026-08-27T00:00:00Z"));

    expect(response.cookies.get(SESSION_COOKIES.access)?.value).toBe("access-token");
    expect(response.headers.getSetCookie().join(";")).toContain("HttpOnly");

    clearSessionCookies(response);
    expect(response.cookies.get(SESSION_COOKIES.access)?.value).toBe("");
  });

  it("reads problem details and falls back for invalid responses", async () => {
    expect(await problemMessage(new Response(JSON.stringify({ detail: "invalid" }), { status: 400 }))).toBe("invalid");
    expect(await problemMessage(new Response("broken", { status: 503 }))).toContain("서비스 연결");
  });
});
