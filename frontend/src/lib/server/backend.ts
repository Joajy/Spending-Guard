import { NextRequest, NextResponse } from "next/server";

export type AuthTokens = {
  tokenType: string;
  accessToken: string;
  accessTokenExpiresAt: string;
  refreshToken: string;
  refreshTokenExpiresAt: string;
  userId: string;
};

export const SESSION_COOKIES = {
  access: "sg_access",
  refresh: "sg_refresh",
  user: "sg_user",
} as const;

export type SessionBackendResult =
  | { authenticated: true; response: Response; refreshed: AuthTokens | null }
  | { authenticated: false };

export function backendUrl(path: string): string {
  const origin = process.env.BACKEND_API_URL ?? "http://localhost:8080";
  return `${origin.replace(/\/$/, "")}${path}`;
}

export function setSessionCookies(
  response: NextResponse,
  tokens: AuthTokens,
  now = Date.now(),
): void {
  const common = {
    httpOnly: true,
    sameSite: "lax" as const,
    secure: process.env.NODE_ENV === "production",
    path: "/",
  };

  response.cookies.set(SESSION_COOKIES.access, tokens.accessToken, {
    ...common,
    maxAge: secondsUntil(tokens.accessTokenExpiresAt, now),
  });
  response.cookies.set(SESSION_COOKIES.refresh, tokens.refreshToken, {
    ...common,
    maxAge: secondsUntil(tokens.refreshTokenExpiresAt, now),
  });
  response.cookies.set(SESSION_COOKIES.user, tokens.userId, {
    ...common,
    maxAge: secondsUntil(tokens.refreshTokenExpiresAt, now),
  });
}

export function clearSessionCookies(response: NextResponse): void {
  Object.values(SESSION_COOKIES).forEach((name) => response.cookies.delete(name));
}

export async function fetchWithSession(
  request: NextRequest,
  perform: (userId: string, accessToken: string) => Promise<Response>,
): Promise<SessionBackendResult> {
  const userId = request.cookies.get(SESSION_COOKIES.user)?.value;
  const accessToken = request.cookies.get(SESSION_COOKIES.access)?.value;
  const refreshToken = request.cookies.get(SESSION_COOKIES.refresh)?.value;
  if (!userId || (!accessToken && !refreshToken)) {
    return { authenticated: false };
  }

  let refreshed: AuthTokens | null = null;
  let activeAccessToken = accessToken;

  if (!activeAccessToken) {
    refreshed = await refreshSession(refreshToken!);
    if (!refreshed || refreshed.userId !== userId) {
      return { authenticated: false };
    }
    activeAccessToken = refreshed.accessToken;
  }

  let response = await perform(userId, activeAccessToken);
  if (response.status === 401 && refreshToken && !refreshed) {
    refreshed = await refreshSession(refreshToken);
    if (!refreshed || refreshed.userId !== userId) {
      return { authenticated: false };
    }
    response = await perform(userId, refreshed.accessToken);
  }

  if (response.status === 401) {
    return { authenticated: false };
  }
  return { authenticated: true, response, refreshed };
}

export async function problemMessage(response: Response): Promise<string> {
  const fallback = response.status >= 500
    ? "서비스 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요."
    : "요청을 처리하지 못했습니다.";

  try {
    const body = await response.json() as { detail?: string; title?: string };
    return body.detail || body.title || fallback;
  } catch {
    return fallback;
  }
}

function secondsUntil(expiresAt: string, now: number): number {
  return Math.max(1, Math.floor((new Date(expiresAt).getTime() - now) / 1000));
}

async function refreshSession(refreshToken: string): Promise<AuthTokens | null> {
  const response = await fetch(backendUrl("/api/v1/auth/refresh"), {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ refreshToken }),
    cache: "no-store",
  });
  if (response.status >= 500 || response.status === 429) {
    throw new Error("Token refresh service is unavailable.");
  }
  return response.ok ? response.json() as Promise<AuthTokens> : null;
}
