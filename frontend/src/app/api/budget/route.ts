import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;
const MAX_BUDGET = 1_000_000_000_000;

export async function GET(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  if (!MONTH_PATTERN.test(month)) {
    return badRequest("조회 월을 확인해 주세요.");
  }
  return relay(request, month, "GET");
}

export async function PUT(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  if (!MONTH_PATTERN.test(month)) {
    return badRequest("조회 월을 확인해 주세요.");
  }

  let body: unknown;
  try {
    body = await request.json();
  } catch {
    return badRequest("설정할 예산을 확인해 주세요.");
  }
  if (!isBudgetRequest(body)) {
    return badRequest("예산 금액과 최신 버전을 확인해 주세요.");
  }
  return relay(request, month, "PUT", body);
}

async function relay(
  request: NextRequest,
  month: string,
  method: "GET" | "PUT",
  body?: { amount: number; version: number | null },
): Promise<NextResponse> {
  try {
    const result = await fetchWithSession(request, (userId, accessToken) =>
      fetch(backendUrl(`/api/v1/users/${userId}/budgets/${month}`), {
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
      { message: method === "GET" ? "예산을 불러오지 못했습니다." : "예산을 저장하지 못했습니다." },
      { status: 503 },
    );
  }
}

function isBudgetRequest(value: unknown): value is { amount: number; version: number | null } {
  if (!value || typeof value !== "object") return false;
  const body = value as Record<string, unknown>;
  const validVersion = body.version === null
    || (typeof body.version === "number" && Number.isInteger(body.version) && body.version >= 0);
  return typeof body.amount === "number"
    && Number.isInteger(body.amount)
    && body.amount >= 1
    && body.amount <= MAX_BUDGET
    && validVersion;
}

function badRequest(message: string): NextResponse {
  return NextResponse.json({ message }, { status: 400 });
}

function unauthorized(): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  clearSessionCookies(response);
  return response;
}
