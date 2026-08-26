import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";

const userId = "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";
const dashboard = {
  month: "2026-08",
  budget: { limitAmount: 1000000, spentAmount: 320000, remainingAmount: 680000 },
  totalSpending: 320000,
  transactionCount: 12,
  categories: [],
  risks: [],
};

function request(cookies = "sg_user=" + userId + "; sg_access=access-token; sg_refresh=refresh-token", month = "2026-08") {
  return new NextRequest(`http://localhost/api/dashboard?month=${month}`, {
    headers: { cookie: cookies },
  });
}

describe("dashboard BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("rejects invalid months and missing sessions", async () => {
    expect((await GET(request("", "2026-13"))).status).toBe(400);
    expect((await GET(request(""))).status).toBe(401);
  });

  it("loads the authenticated user's monthly dashboard", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(dashboard), { status: 200 }),
    );

    const response = await GET(request());
    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(dashboard);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/dashboard?month=2026-08`,
      expect.objectContaining({ headers: { Authorization: "Bearer access-token" } }),
    );
  });

  it("rotates tokens and retries once after an expired access token", async () => {
    const refreshed = {
      tokenType: "Bearer",
      accessToken: "new-access",
      accessTokenExpiresAt: "2099-08-27T00:15:00Z",
      refreshToken: "new-refresh",
      refreshTokenExpiresAt: "2099-09-10T00:00:00Z",
      userId,
    };
    const backend = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(refreshed), { status: 200 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(dashboard), { status: 200 }));

    const response = await GET(request());
    expect(response.status).toBe(200);
    expect(backend).toHaveBeenCalledTimes(3);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_access=new-access");
  });

  it("clears the session when refresh is rejected", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }));

    const response = await GET(request());
    expect(response.status).toBe(401);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_access=");
  });
});
