import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { POST, PUT } from "./route";

const cookie = "sg_onboarding_user=f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";

function request(method: "POST" | "PUT", body?: unknown, withCookie = true): NextRequest {
  return new NextRequest("http://localhost/api/email-verification", {
    method,
    headers: {
      ...(withCookie ? { cookie } : {}),
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

describe("email verification BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("does not accept a verification request without an onboarding session", async () => {
    const backend = vi.spyOn(globalThis, "fetch");
    const response = await POST(request("POST", undefined, false));
    expect(response.status).toBe(401);
    expect((await response.json()).message).toContain("회원가입을 다시");
    expect(backend).not.toHaveBeenCalled();
  });

  it("issues a verification code for the server-held user identifier", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 202 }));
    const response = await POST(request("POST"));
    expect(response.status).toBe(202);
    expect(backend).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/users/f0e6cfcc-7d18-43f1-ae90-a60eb83b005c/email-verification",
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("explains the resend cooldown", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 429 }));
    const response = await POST(request("POST"));
    expect(response.status).toBe(429);
    expect((await response.json()).message).toContain("60초");
  });

  it.each([{}, { code: "12ab56" }, { code: "12345" }])(
    "rejects a malformed confirmation code: %o",
    async (body) => {
      const backend = vi.spyOn(globalThis, "fetch");
      const response = await PUT(request("PUT", body));
      expect(response.status).toBe(400);
      expect(backend).not.toHaveBeenCalled();
    },
  );

  it("confirms the code and removes the one-time onboarding cookie", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
    const response = await PUT(request("PUT", { code: "123456" }));
    expect(response.status).toBe(204);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_onboarding_user=");
    expect(backend).toHaveBeenCalledWith(
      expect.stringContaining("/confirmation"),
      expect.objectContaining({ body: JSON.stringify({ code: "123456" }) }),
    );
  });

  it.each([
    [422, "올바르지 않거나 만료"],
    [429, "입력 횟수를 초과"],
  ])("maps confirmation failure %i", async (status, message) => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status }));
    const response = await PUT(request("PUT", { code: "123456" }));
    expect(response.status).toBe(status);
    expect((await response.json()).message).toContain(message);
  });

  it("clears stale onboarding state when the account no longer exists", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 404 }));
    const response = await POST(request("POST"));
    expect(response.status).toBe(404);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_onboarding_user=");
  });

  it("returns a retryable response when the mail service is unreachable", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));
    const response = await POST(request("POST"));
    expect(response.status).toBe(503);
    expect((await response.json()).message).toContain("잠시 후");
  });
});
