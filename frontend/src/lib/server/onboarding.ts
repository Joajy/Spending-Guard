import { NextRequest, NextResponse } from "next/server";

export const ONBOARDING_COOKIE = "sg_onboarding_user";

export function onboardingUserId(request: NextRequest): string | null {
  return request.cookies.get(ONBOARDING_COOKIE)?.value ?? null;
}

export function setOnboardingCookie(response: NextResponse, userId: string): void {
  response.cookies.set(ONBOARDING_COOKIE, userId, {
    httpOnly: true,
    sameSite: "lax",
    secure: process.env.NODE_ENV === "production",
    path: "/",
    maxAge: 30 * 60,
  });
}

export function clearOnboardingCookie(response: NextResponse): void {
  response.cookies.delete(ONBOARDING_COOKIE);
}
