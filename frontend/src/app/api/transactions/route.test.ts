import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GET } from "./route";

const userId = "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";
const session = `sg_user=${userId}; sg_access=access-token; sg_refresh=refresh-token`;
const page = { items: [], nextCursor: null, hasNext: false };

function request(query: string, cookies = session) {
  return new NextRequest(`http://localhost/api/transactions?${query}`, {
    headers: { cookie: cookies },
  });
}

describe("transaction history BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("rejects invalid filters before calling the backend", async () => {
    const backend = vi.spyOn(globalThis, "fetch");

    expect((await GET(request("month=2026-13"))).status).toBe(400);
    expect((await GET(request("month=2026-08&status=DONE"))).status).toBe(400);
    expect((await GET(request("month=2026-08&category=FOOD"))).status).toBe(400);
    expect(backend).not.toHaveBeenCalled();
  });

  it("clears an incomplete session", async () => {
    const response = await GET(request("month=2026-08", ""));

    expect(response.status).toBe(401);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_access=");
  });

  it("forwards filters and the cursor for the authenticated user", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(page), { status: 200 }),
    );

    const response = await GET(request(
      "month=2026-08&status=NEEDS_REVIEW&category=DELIVERY&cursor=next-page",
    ));

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(page);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/spend-events?month=2026-08&size=20&status=NEEDS_REVIEW&category=DELIVERY&cursor=next-page`,
      expect.objectContaining({ headers: { Authorization: "Bearer access-token" } }),
    );
  });

  it("returns a retryable response when the backend cannot be reached", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));

    const response = await GET(request("month=2026-08"));

    expect(response.status).toBe(503);
    expect(await response.json()).toEqual({ message: "소비 내역을 불러오지 못했습니다." });
  });
});
