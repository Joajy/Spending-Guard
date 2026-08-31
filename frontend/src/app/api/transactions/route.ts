import { NextRequest, NextResponse } from "next/server";
import {
  backendUrl,
  clearSessionCookies,
  fetchWithSession,
  problemMessage,
  setSessionCookies,
} from "@/lib/server/backend";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;
const STATUSES = new Set(["RECEIVED", "ANALYZING", "NEEDS_REVIEW"]);
const CATEGORIES = new Set([
  "SUBSCRIPTION",
  "TRANSPORT",
  "DELIVERY",
  "SHOPPING",
  "TRANSFER",
  "OTHER",
]);

export async function GET(request: NextRequest): Promise<NextResponse> {
  const month = request.nextUrl.searchParams.get("month") ?? "";
  const status = request.nextUrl.searchParams.get("status") ?? "";
  const category = request.nextUrl.searchParams.get("category") ?? "";
  const cursor = request.nextUrl.searchParams.get("cursor") ?? "";

  if (!MONTH_PATTERN.test(month)) {
    return badRequest("조회 월을 확인해 주세요.");
  }
  if (status && !STATUSES.has(status)) {
    return badRequest("처리 상태를 확인해 주세요.");
  }
  if (category && !CATEGORIES.has(category)) {
    return badRequest("카테고리를 확인해 주세요.");
  }

  const query = new URLSearchParams({ month, size: "20" });
  if (status) query.set("status", status);
  if (category) query.set("category", category);
  if (cursor) query.set("cursor", cursor);

  try {
    const result = await fetchWithSession(request, (userId, accessToken) =>
      fetch(backendUrl(`/api/v1/users/${userId}/spend-events?${query}`), {
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
      { message: "소비 내역을 불러오지 못했습니다." },
      { status: 503 },
    );
  }
}

type SubmissionBody = { message?: string; occurredAt?: string | null };

export async function POST(request: NextRequest): Promise<NextResponse> {
  let body: SubmissionBody;
  try {
    body = await request.json() as SubmissionBody;
  } catch {
    return badRequest("소비 알림 내용을 확인해 주세요.");
  }

  const message = body.message?.trim() ?? "";
  if (!message || message.length > 2_000) {
    return badRequest("소비 알림은 1~2,000자로 입력해 주세요.");
  }
  if (body.occurredAt && Number.isNaN(Date.parse(body.occurredAt))) {
    return badRequest("결제 발생 시각을 확인해 주세요.");
  }

  try {
    const result = await fetchWithSession(request, (userId, accessToken) =>
      fetch(backendUrl(`/api/v1/users/${userId}/spend-events`), {
        method: "POST",
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          source: "MANUAL_TEXT",
          message,
          occurredAt: body.occurredAt || null,
        }),
        cache: "no-store",
      }));

    if (!result.authenticated) return unauthorized();
    if (!result.response.ok) {
      return NextResponse.json(
        { message: await problemMessage(result.response) },
        { status: result.response.status },
      );
    }

    const response = NextResponse.json(await result.response.json(), { status: 202 });
    if (result.refreshed) setSessionCookies(response, result.refreshed);
    return response;
  } catch {
    return NextResponse.json(
      { message: "소비 알림을 접수하지 못했습니다. 잠시 후 다시 시도해 주세요." },
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
