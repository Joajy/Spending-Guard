import { NextRequest, NextResponse } from "next/server";
import { backendUrl, problemMessage } from "@/lib/server/backend";
import { setOnboardingCookie } from "@/lib/server/onboarding";

type RegistrationBody = { email?: string; password?: string };
type RegistrationResult = { userId: string; email: string };

export async function POST(request: NextRequest): Promise<NextResponse> {
  let body: RegistrationBody;
  try {
    body = await request.json() as RegistrationBody;
  } catch {
    return invalidRegistration();
  }

  const email = body.email?.trim() ?? "";
  const password = body.password ?? "";
  if (!isValidEmail(email) || password.length < 8 || password.length > 72) {
    return invalidRegistration();
  }

  try {
    const backendResponse = await fetch(backendUrl("/api/v1/users"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email, password }),
      cache: "no-store",
    });

    if (!backendResponse.ok) {
      return NextResponse.json(
        { message: await registrationFailureMessage(backendResponse) },
        { status: backendResponse.status },
      );
    }

    const registered = await backendResponse.json() as RegistrationResult;
    const response = NextResponse.json({ email: registered.email }, { status: 201 });
    setOnboardingCookie(response, registered.userId);
    return response;
  } catch {
    return NextResponse.json(
      { message: "회원가입 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." },
      { status: 503 },
    );
  }
}

function invalidRegistration(): NextResponse {
  return NextResponse.json(
    { message: "이메일 형식과 8~72자의 비밀번호를 확인해 주세요." },
    { status: 400 },
  );
}

async function registrationFailureMessage(response: Response): Promise<string> {
  if (response.status === 409) return "이미 가입된 이메일입니다.";
  if (response.status === 400) return "이메일 형식과 비밀번호 조건을 확인해 주세요.";
  return problemMessage(response);
}

function isValidEmail(email: string): boolean {
  return email.length <= 320 && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
}
