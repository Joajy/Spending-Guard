import { NextRequest } from "next/server";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { POST as createOrder } from "./route";
import { POST as confirmOrder } from "./[orderId]/confirm/route";
import { POST as cancelOrder } from "./[orderId]/cancel/route";

const userId = "f0e6cfcc-7d18-43f1-ae90-a60eb83b005c";
const cookies = `sg_user=${userId}; sg_access=access-token; sg_refresh=refresh-token`;

function request(path: string, body: object, authenticated = true): NextRequest {
  return new NextRequest(`http://localhost${path}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(authenticated ? { cookie: cookies } : {}),
    },
    body: JSON.stringify(body),
  });
}

describe("Toss test BFF routes", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("creates an authenticated user's test order", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(
      JSON.stringify({ orderId: "sg_order123", clientKey: "test_ck", amount: 12_800 }),
      { status: 200 },
    ));

    const response = await createOrder(request("/api/toss-test/orders", { amount: 12_800 }));

    expect(response.status).toBe(200);
    expect(backend).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/users/${userId}/integrations/toss-payments/test-orders`,
      expect.objectContaining({ method: "POST" }),
    );
  });

  it("forwards confirmation and cancellation to the same owned order", async () => {
    const backend = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(
      JSON.stringify({ status: "DONE" }),
      { status: 200 },
    ));
    const context = { params: Promise.resolve({ orderId: "sg_order123" }) };

    expect((await confirmOrder(request("/confirm", { paymentKey: "pk", amount: 12_800 }), context)).status)
      .toBe(200);
    expect((await cancelOrder(request("/cancel", { paymentKey: "pk" }), context)).status)
      .toBe(200);
    expect(backend.mock.calls[0][0]).toContain("/sg_order123/confirm");
    expect(backend.mock.calls[1][0]).toContain("/sg_order123/cancel");
  });

  it("rejects a request without a session", async () => {
    const backend = vi.spyOn(globalThis, "fetch");

    const response = await createOrder(request("/api/toss-test/orders", { amount: 12_800 }, false));

    expect(response.status).toBe(401);
    expect(backend).not.toHaveBeenCalled();
  });
});
