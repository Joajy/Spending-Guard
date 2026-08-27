import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DELETE, POST } from "./route";

const tokens = {
  tokenType: "Bearer",
  accessToken: "access-token",
  accessTokenExpiresAt: "2099-08-27T00:15:00Z",
  refreshToken: "refresh-token",
  refreshTokenExpiresAt: "2099-09-10T00:00:00Z",
  userId: "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c",
};

function loginRequest(body: unknown): NextRequest {
  return new NextRequest("http://localhost/api/session", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
}

describe("session BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("logs in and stores tokens in HttpOnly cookies", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(tokens), { status: 200 }),
    );

    const response = await POST(loginRequest({ email: " member@example.com ", password: "password" }));

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual({ userId: tokens.userId });
    expect(response.headers.getSetCookie().join(";")).toContain("sg_access=access-token");
    expect(response.headers.getSetCookie().join(";")).toContain("HttpOnly");
    expect(globalThis.fetch).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/auth/login",
      expect.objectContaining({
        body: JSON.stringify({ email: "member@example.com", password: "password" }),
      }),
    );
  });

  it("returns a user-facing message for rejected and invalid requests", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 401 }));
    const rejected = await POST(loginRequest({ email: "member@example.com", password: "wrong" }));
    expect(rejected.status).toBe(401);
    expect(await rejected.json()).toEqual({ message: "이메일 또는 비밀번호가 올바르지 않습니다." });

    const invalid = await POST(loginRequest({ email: "", password: "" }));
    expect(invalid.status).toBe(400);
  });

  it("clears the browser session even when backend logout fails", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));
    const request = new NextRequest("http://localhost/api/session", {
      method: "DELETE",
      headers: { cookie: "sg_refresh=refresh-token; sg_access=access-token; sg_user=user-1" },
    });

    const response = await DELETE(request);
    expect(response.status).toBe(204);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_refresh=");
  });
});
