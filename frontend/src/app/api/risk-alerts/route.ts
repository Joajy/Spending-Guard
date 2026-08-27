import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;
const MINIMUM_LEVELS = new Set(["MEDIUM", "HIGH"]);

export async function GET(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  const minimumLevel = request.nextUrl.searchParams.get("minimumLevel") ?? "MEDIUM";
  const cursor = request.nextUrl.searchParams.get("cursor") ?? "";

  if (!MONTH_PATTERN.test(month)) {
    return badRequest("조회 월을 확인해 주세요.");
  }
  if (!MINIMUM_LEVELS.has(minimumLevel)) {
    return badRequest("위험도 필터를 확인해 주세요.");
  }

  const query = new URLSearchParams({ month, minimumLevel, size: "20" });
  if (cursor) query.set("cursor", cursor);

  try {
    const result = await fetchWithSession(request, (userId, accessToken) =>
      fetch(backendUrl(`/api/v1/users/${userId}/risk-alerts?${query}`), {
        headers: { Authorization: `Bearer ${accessToken}` },
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
      { message: "위험 알림을 불러오지 못했습니다." },
      { status: 503 },
    );
  }
}

function badRequest(message: string): NextResponse {
  return NextResponse.json({ message }, { status: 400 });
}

function unauthorized(): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  clearSessionCookies(response);
  return response;
}
