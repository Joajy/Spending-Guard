import { NextRequest, NextResponse } from "next/server";
import { backendUrl, problemMessage } from "@/lib/server/backend";
import {
  clearOnboardingCookie,
  onboardingUserId,
} from "@/lib/server/onboarding";

type ConfirmationBody = { code?: string };

export async function POST(request: NextRequest): Promise<NextResponse> {
  return withOnboardingUser(request, async (userId) => {
    const backendResponse = await fetch(
      backendUrl(`/api/v1/users/${userId}/email-verification`),
      { method: "POST", cache: "no-store" },
    );

    if (!backendResponse.ok) {
      return verificationFailure(backendResponse, "issue");
    }
    return new NextResponse(null, { status: 202 });
  });
}

export async function PUT(request: NextRequest): Promise<NextResponse> {
  let body: ConfirmationBody;
  try {
    body = await request.json() as ConfirmationBody;
  } catch {
    return invalidCode();
  }

  const code = body.code?.trim() ?? "";
  if (!/^\d{6}$/.test(code)) return invalidCode();

  return withOnboardingUser(request, async (userId) => {
    const backendResponse = await fetch(
      backendUrl(`/api/v1/users/${userId}/email-verification/confirmation`),
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
        cache: "no-store",
      },
    );

    if (!backendResponse.ok) {
      return verificationFailure(backendResponse, "confirm");
    }

    const response = new NextResponse(null, { status: 204 });
    clearOnboardingCookie(response);
    return response;
  });
}

async function withOnboardingUser(
  request: NextRequest,
  perform: (userId: string) => Promise<NextResponse>,
): Promise<NextResponse> {
  const userId = onboardingUserId(request);
  if (!userId) {
    return NextResponse.json(
      { message: "인증 정보가 만료되었습니다. 회원가입을 다시 진행해 주세요." },
      { status: 401 },
    );
  }

  try {
    return await perform(userId);
  } catch {
    return NextResponse.json(
      { message: "이메일 인증 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요." },
      { status: 503 },
    );
  }
}

async function verificationFailure(
  backendResponse: Response,
  operation: "issue" | "confirm",
): Promise<NextResponse> {
  let message: string;
  if (backendResponse.status === 429) {
    message = operation === "issue"
      ? "인증번호는 60초 후 다시 요청할 수 있습니다."
      : "입력 횟수를 초과했습니다. 새 인증번호를 요청해 주세요.";
  } else if (backendResponse.status === 422) {
    message = "인증번호가 올바르지 않거나 만료되었습니다.";
  } else if (backendResponse.status === 404) {
    message = "가입 정보를 찾을 수 없습니다. 회원가입을 다시 진행해 주세요.";
  } else {
    message = await problemMessage(backendResponse);
  }

  const response = NextResponse.json({ message }, { status: backendResponse.status });
  if (backendResponse.status === 404) clearOnboardingCookie(response);
  return response;
}

function invalidCode(): NextResponse {
  return NextResponse.json(
    { message: "이메일로 받은 6자리 인증번호를 입력해 주세요." },
    { status: 400 },
  );
}
