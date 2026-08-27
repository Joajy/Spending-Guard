import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

export async function GET(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  if (!MONTH_PATTERN.test(month)) {
    return NextResponse.json({ message: "조회 월을 확인해 주세요." }, { status: 400 });
  }

  try {
    const result = await fetchWithSession(
      request,
      (userId, accessToken) => fetchDashboard(userId, month, accessToken),
    );
    if (!result.authenticated) {
      return unauthorized(true);
    }

    const dashboardResponse = result.response;

    if (!dashboardResponse.ok) {
      return NextResponse.json(
        { message: await problemMessage(dashboardResponse) },
        { status: dashboardResponse.status },
      );
    }

    const response = NextResponse.json(await dashboardResponse.json());
    if (result.refreshed) {
      setSessionCookies(response, result.refreshed);
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

function unauthorized(clear = false): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  if (clear) {
    clearSessionCookies(response);
  }
  return response;
}
