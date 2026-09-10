import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

export async function POST(request: NextRequest): Promise<NextResponse> {
  const body = await request.text();
  try {
    const result = await fetchWithSession(request, (userId, accessToken) => fetch(
      backendUrl(`/api/v1/users/${userId}/integrations/toss-payments/test-orders`),
      {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body,
        cache: "no-store",
      },
    ));
    if (!result.authenticated) return unauthorized();
    if (!result.response.ok) {
      return NextResponse.json(
        { message: await problemMessage(result.response) },
        { status: result.response.status },
      );
    }
    const response = NextResponse.json(await result.response.json());
    if (result.refreshed) setSessionCookies(response, result.refreshed);
    return response;
  } catch {
    return NextResponse.json({ message: "Toss 테스트 주문을 만들지 못했습니다." }, { status: 503 });
  }
}

function unauthorized(): NextResponse {
  const response = NextResponse.json({ message: "로그인이 필요합니다." }, { status: 401 });
  clearSessionCookies(response);
  return response;
}
