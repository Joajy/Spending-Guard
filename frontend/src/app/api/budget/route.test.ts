import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GET, PUT } from "./route";

const userId = "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";
const cookies = `sg_user=${userId}; sg_access=access-token; sg_refresh=refresh-token`;
const budget = {
  month: "2026-08",
  limitAmount: 1_000_000,
  spentAmount: 320_000,
  remainingAmount: 680_000,
  version: 2,
  updatedAt: "2026-08-28T10:00:00Z",
};

function request(method: "GET" | "PUT", month = "2026-08", body?: unknown, cookie = cookies) {
  return new NextRequest(`http://localhost/api/budget?month=${month}`, {
    method,
    headers: { cookie, ...(body ? { "Content-Type": "application/json" } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
}

describe("budget BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("rejects invalid months, malformed values and missing sessions", async () => {
    expect((await GET(request("GET", "2026-13"))).status).toBe(400);
    expect((await GET(request("GET", "2026-08", undefined, ""))).status).toBe(401);
    expect((await PUT(request("PUT", "2026-08", { amount: 0, version: null }))).status).toBe(400);
    expect((await PUT(request("PUT", "2026-08", { amount: 1_000_000, version: -1 }))).status).toBe(400);
  });

  it("loads the authenticated user's monthly budget", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(budget), { status: 200 }),
    );

    const response = await GET(request("GET"));

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(budget);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/budgets/2026-08`,
      expect.objectContaining({ method: "GET", headers: { Authorization: "Bearer access-token" } }),
    );
  });

  it("forwards the current version when saving a budget", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ...budget, limitAmount: 1_500_000, version: 3 }), { status: 200 }),
    );

    const response = await PUT(request("PUT", "2026-08", { amount: 1_500_000, version: 2 }));

    expect(response.status).toBe(200);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/budgets/2026-08`,
      expect.objectContaining({
        method: "PUT",
        body: JSON.stringify({ amount: 1_500_000, version: 2 }),
      }),
    );
  });

  it("preserves an optimistic lock conflict from the backend", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ detail: "예산이 먼저 변경되었습니다." }), { status: 409 }),
    );

    const response = await PUT(request("PUT", "2026-08", { amount: 900_000, version: 1 }));

    expect(response.status).toBe(409);
    expect(await response.json()).toEqual({ message: "예산이 먼저 변경되었습니다." });
  });

  it("returns a stable message when the backend is unavailable", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("network"));

    expect((await GET(request("GET"))).status).toBe(503);
    expect((await PUT(request("PUT", "2026-08", { amount: 900_000, version: null }))).status).toBe(503);
  });
});
