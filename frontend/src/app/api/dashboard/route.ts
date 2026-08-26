import { NextRequest, NextResponse } from "next/server";
import {
  AuthTokens,
  backendUrl,
  clearSessionCookies,
  problemMessage,
  SESSION_COOKIES,
  setSessionCookies,
} from "@/lib/server/backend";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

export async function GET(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  if (!MONTH_PATTERN.test(month)) {
    return NextResponse.json({ message: "조회 월을 확인해 주세요." }, { status: 400 });
  }

  const userId = request.cookies.get(SESSION_COOKIES.user)?.value;
  const accessToken = request.cookies.get(SESSION_COOKIES.access)?.value;
  const refreshToken = request.cookies.get(SESSION_COOKIES.refresh)?.value;
  if (!userId || !accessToken) {
    return unauthorized();
  }

  try {
    let dashboardResponse = await fetchDashboard(userId, month, accessToken);
    let refreshed: AuthTokens | null = null;

    if (dashboardResponse.status === 401 && refreshToken) {
      refreshed = await refreshSession(refreshToken);
      if (!refreshed || refreshed.userId !== userId) {
        return unauthorized(true);
      }
      dashboardResponse = await fetchDashboard(userId, month, refreshed.accessToken);
    }

    if (!dashboardResponse.ok) {
      return NextResponse.json(
        { message: await problemMessage(dashboardResponse) },
        { status: dashboardResponse.status },
      );
    }

    const response = NextResponse.json(await dashboardResponse.json());
    if (refreshed) {
      setSessionCookies(response, refreshed);
    }
    return response;
  } catch {
    return NextResponse.json(
      { message: "대시보드 데이터를 불러오지 못했습니다." },
      { status: 503 },
    );
  }
}

async function fetchDashboard(userId: string, month: string, accessToken: string): Promise<Response> {
  const query = new URLSearchParams({ month });
  return fetch(backendUrl(`/api/v1/users/${userId}/dashboard?${query}`), {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: "no-store",
  });
}

async function refreshSession(refreshToken: string): Promise<AuthTokens | null> {
  const response = await fetch(backendUrl("/api/v1/auth/refresh"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store",
  });
  return response.ok ? response.json() as Promise<AuthTokens> : null;
}

function unauthorized(clear = false): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  if (clear) {
    clearSessionCookies(response);
  }
  return response;
}
