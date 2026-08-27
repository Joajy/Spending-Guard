import { NextRequest, NextResponse } from "next/server";
import {
  AuthTokens,
  backendUrl,
  clearSessionCookies,
  problemMessage,
  SESSION_COOKIES,
  setSessionCookies,
} from "@/lib/server/backend";

type LoginBody = { email?: string; password?: string };

export async function POST(request: NextRequest): Promise<NextResponse> {
  let body: LoginBody;
  try {
    body = await request.json() as LoginBody;
  } catch {
    return NextResponse.json({ message: "이메일과 비밀번호를 확인해 주세요." }, { status: 400 });
  }

  if (!body.email?.trim() || !body.password) {
    return NextResponse.json({ message: "이메일과 비밀번호를 입력해 주세요." }, { status: 400 });
  }

  try {
    const backendResponse = await fetch(backendUrl("/api/v1/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: body.email.trim(), password: body.password }),
      cache: "no-store",
    });

    if (!backendResponse.ok) {
      return NextResponse.json(
        { message: await loginFailureMessage(backendResponse) },
        { status: backendResponse.status },
      );
    }

    const tokens = await backendResponse.json() as AuthTokens;
    const response = NextResponse.json({ userId: tokens.userId });
    setSessionCookies(response, tokens);
    return response;
  } catch {
    return NextResponse.json(
      { message: "백엔드 서비스에 연결할 수 없습니다." },
      { status: 503 },
    );
  }
}

async function loginFailureMessage(response: Response): Promise<string> {
  if (response.status === 401) return "이메일 또는 비밀번호가 올바르지 않습니다.";
  if (response.status === 403) return "이메일 인증을 완료한 뒤 로그인해 주세요.";
  return problemMessage(response);
}

export async function DELETE(request: NextRequest): Promise<NextResponse> {
  const refreshToken = request.cookies.get(SESSION_COOKIES.refresh)?.value;

  if (refreshToken) {
    try {
      await fetch(backendUrl("/api/v1/auth/logout"), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
        cache: "no-store",
      });
    } catch {
      // The local session must still be cleared when the backend is unavailable.
    }
  }

  const response = new NextResponse(null, { status: 204 });
  clearSessionCookies(response);
  return response;
}
