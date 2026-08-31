import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { POST } from "./route";

function request(body: unknown): NextRequest {
  return new NextRequest("http://localhost/api/registration", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: typeof body === "string" ? body : JSON.stringify(body),
  });
}

describe("registration BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("registers a user and stores only the onboarding identifier in an HttpOnly cookie", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({
        userId: "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c",
        email: "member@example.com",
        createdAt: "2026-08-31T10:00:00Z",
      }), { status: 201 }),
    );

    const response = await POST(request({ email: " member@example.com ", password: "safe-password" }));

    expect(response.status).toBe(201);
    expect(await response.json()).toEqual({ email: "member@example.com" });
    expect(response.headers.getSetCookie().join(";")).toContain("sg_onboarding_user=f0e6cfcc");
    expect(response.headers.getSetCookie().join(";")).toContain("HttpOnly");
    expect(backend).toHaveBeenCalledWith("http://localhost:8080/api/v1/users", expect.objectContaining({
      body: JSON.stringify({ email: "member@example.com", password: "safe-password" }),
    }));
  });

  it.each([
    ["malformed JSON", "{"],
    ["invalid email", { email: "wrong", password: "safe-password" }],
    ["short password", { email: "member@example.com", password: "short" }],
    ["BCrypt boundary", { email: "member@example.com", password: "a".repeat(73) }],
  ])("rejects %s before calling the backend", async (_case, body) => {
    const backend = vi.spyOn(globalThis, "fetch");
    const response = await POST(request(body));
    expect(response.status).toBe(400);
    expect(backend).not.toHaveBeenCalled();
  });

  it("maps duplicate accounts to a stable user-facing message", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 409 }));
    const response = await POST(request({ email: "member@example.com", password: "safe-password" }));
    expect(response.status).toBe(409);
    expect(await response.json()).toEqual({ message: "이미 가입된 이메일입니다." });
  });

  it("returns a retryable response when the backend is unavailable", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));
    const response = await POST(request({ email: "member@example.com", password: "safe-password" }));
    expect(response.status).toBe(503);
    expect((await response.json()).message).toContain("잠시 후");
  });
});
