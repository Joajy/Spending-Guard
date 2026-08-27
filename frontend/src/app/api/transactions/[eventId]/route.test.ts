import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { GET, PATCH } from "./route";

const userId = "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";
const eventId = "11111111-1111-4111-8111-111111111111";
const session = `sg_user=${userId}; sg_access=access-token; sg_refresh=refresh-token`;
const context = { params: Promise.resolve({ eventId }) };

function request(method = "GET", body?: unknown, cookies = session) {
  return new NextRequest(`http://localhost/api/transactions/${eventId}`, {
    method,
    headers: {
      cookie: cookies,
      ...(body ? { "Content-Type": "application/json" } : {}),
    },
    ...(body ? { body: JSON.stringify(body) } : {}),
  });
}

describe("transaction detail BFF route", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("rejects an invalid event id before calling the backend", async () => {
    const backend = vi.spyOn(globalThis, "fetch");

    const response = await GET(request(), { params: Promise.resolve({ eventId: "invalid" }) });

    expect(response.status).toBe(400);
    expect(backend).not.toHaveBeenCalled();
  });

  it("loads the authenticated user's transaction detail", async () => {
    const detail = { eventId, status: "ANALYZING", fastParse: null };
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(detail), { status: 200 }),
    );

    const response = await GET(request(), context);

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(detail);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/spend-events/${eventId}`,
      expect.objectContaining({
        method: "GET",
        headers: { Authorization: "Bearer access-token" },
      }),
    );
  });

  it("validates category corrections before forwarding them", async () => {
    const backend = vi.spyOn(globalThis, "fetch");

    const response = await PATCH(request("PATCH", { category: "FOOD", expectedVersion: -1 }), context);

    expect(response.status).toBe(400);
    expect(backend).not.toHaveBeenCalled();
  });

  it("forwards the category and expected version", async () => {
    const correction = { eventId, category: "DELIVERY", version: 2 };
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify(correction), { status: 200 }),
    );
    const body = { category: "DELIVERY", expectedVersion: 1 };

    const response = await PATCH(request("PATCH", body), context);

    expect(response.status).toBe(200);
    expect(await response.json()).toEqual(correction);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/spend-events/${eventId}/category`,
      expect.objectContaining({
        method: "PATCH",
        headers: {
          Authorization: "Bearer access-token",
          "Content-Type": "application/json",
        },
        body: JSON.stringify(body),
      }),
    );
  });

  it("preserves a version conflict message for the screen", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({
      detail: "다른 요청이 먼저 카테고리를 수정했습니다. 최신 내역을 다시 조회해 주세요.",
    }), { status: 409 }));

    const response = await PATCH(request("PATCH", {
      category: "DELIVERY",
      expectedVersion: 1,
    }), context);

    expect(response.status).toBe(409);
    expect(await response.json()).toEqual({
      message: "다른 요청이 먼저 카테고리를 수정했습니다. 최신 내역을 다시 조회해 주세요.",
    });
  });

  it("clears an incomplete detail session", async () => {
    const response = await GET(request("GET", undefined, ""), context);

    expect(response.status).toBe(401);
    expect(response.headers.getSetCookie().join(";")).toContain("sg_access=");
  });

  it("returns a retryable response when a correction cannot reach the backend", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));

    const response = await PATCH(request("PATCH", {
      category: "DELIVERY",
      expectedVersion: 1,
    }), context);

    expect(response.status).toBe(503);
    expect(await response.json()).toEqual({ message: "카테고리를 수정하지 못했습니다." });
  });
});
