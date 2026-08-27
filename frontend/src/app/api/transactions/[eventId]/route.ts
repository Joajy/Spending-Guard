import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

const EVENT_ID_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
const CATEGORIES = new Set([
  "SUBSCRIPTION",
  "TRANSPORT",
  "DELIVERY",
  "SHOPPING",
  "TRANSFER",
  "OTHER",
]);

type RouteContext = { params: Promise<{ eventId: string }> };

export async function GET(request: NextRequest, context: RouteContext): Promise<NextResponse> {
  const { eventId } = await context.params;
  if (!EVENT_ID_PATTERN.test(eventId)) {
    return badRequest("소비 내역 식별자를 확인해 주세요.");
  }
  return relay(request, eventId, "GET");
}

export async function PATCH(request: NextRequest, context: RouteContext): Promise<NextResponse> {
  const { eventId } = await context.params;
  if (!EVENT_ID_PATTERN.test(eventId)) {
    return badRequest("소비 내역 식별자를 확인해 주세요.");
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return badRequest("수정할 카테고리를 확인해 주세요.");
  }
  if (!isCorrectionRequest(body)) {
    return badRequest("수정할 카테고리와 최신 버전을 확인해 주세요.");
  }
  return relay(request, eventId, "PATCH", body);
}

async function relay(
  request: NextRequest,
  eventId: string,
  method: "GET" | "PATCH",
  body?: { category: string; expectedVersion: number },
): Promise<NextResponse> {
  try {
    const result = await fetchWithSession(request, (userId, accessToken) =>
      fetch(backendUrl(
        `/api/v1/users/${userId}/spend-events/${eventId}${method === "PATCH" ? "/category" : ""}`,
      ), {
        method,
        headers: {
          Authorization: `Bearer ${accessToken}`,
          ...(body ? { "Content-Type": "application/json" } : {}),
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
        cache: "no-store",
      }));

    if (!result.authenticated) {
      return unauthorized();
    }
    if (!result.response.ok) {
      return NextResponse.json(
        { message: await problemMessage(result.response) },
        { status: result.response.status },
      );
    }

    const response = NextResponse.json(await result.response.json());
    if (result.refreshed) {
      setSessionCookies(response, result.refreshed);
    }
    return response;
  } catch {
    return NextResponse.json(
      { message: method === "GET" ? "소비 상세를 불러오지 못했습니다." : "카테고리를 수정하지 못했습니다." },
      { status: 503 },
    );
  }
}

function isCorrectionRequest(value: unknown): value is { category: string; expectedVersion: number } {
  if (!value || typeof value !== "object") return false;
  const body = value as Record<string, unknown>;
  return typeof body.category === "string"
    && CATEGORIES.has(body.category)
    && typeof body.expectedVersion === "number"
    && Number.isInteger(body.expectedVersion)
    && body.expectedVersion >= 0;
}

function badRequest(message: string): NextResponse {
  return NextResponse.json({ message }, { status: 400 });
}

function unauthorized(): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  clearSessionCookies(response);
  return response;
}
