"use client";

import Link from "next/link";
import { FormEvent, useEffect, useState } from "react";

type VerifyEmailFormProps = { initiallySent: boolean };
type PendingAction = "confirm" | "resend" | null;

export function VerifyEmailForm({ initiallySent }: VerifyEmailFormProps) {
  const [code, setCode] = useState("");
  const [pending, setPending] = useState<PendingAction>(null);
  const [remainingSeconds, setRemainingSeconds] = useState(initiallySent ? 60 : 0);
  const [message, setMessage] = useState(
    initiallySent ? "인증번호를 발송했습니다. 5분 안에 입력해 주세요." : "인증번호 발송을 다시 요청해 주세요.",
  );
  const [error, setError] = useState(!initiallySent);
  const [completed, setCompleted] = useState(false);

  useEffect(() => {
    if (remainingSeconds <= 0) return;
    const timer = window.setInterval(() => {
      setRemainingSeconds((current) => Math.max(0, current - 1));
    }, 1_000);
    return () => window.clearInterval(timer);
  }, [remainingSeconds]);

  async function confirm(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!/^\d{6}$/.test(code)) {
      setError(true);
      setMessage("이메일로 받은 6자리 인증번호를 입력해 주세요.");
      return;
    }

    setPending("confirm");
    setMessage("");
    try {
      const response = await fetch("/api/email-verification", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ code }),
      });
      if (!response.ok) {
        const body = await response.json() as { message?: string };
        setError(true);
        setMessage(body.message ?? "인증번호를 확인하지 못했습니다.");
        return;
      }
      setCompleted(true);
      setError(false);
      setMessage("이메일 인증이 완료되었습니다.");
    } catch {
      setError(true);
      setMessage("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setPending(null);
    }
  }

  async function resend() {
    setPending("resend");
    setMessage("");
    try {
      const response = await fetch("/api/email-verification", { method: "POST" });
      if (!response.ok) {
        const body = await response.json() as { message?: string };
        setError(true);
        setMessage(body.message ?? "인증번호를 발송하지 못했습니다.");
        return;
      }
      setRemainingSeconds(60);
      setError(false);
      setMessage("새 인증번호를 발송했습니다. 5분 안에 입력해 주세요.");
    } catch {
      setError(true);
      setMessage("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setPending(null);
    }
  }

  if (completed) {
    return (
      <div className="verification-complete" role="status">
        <span aria-hidden="true">✓</span>
        <h3>인증을 마쳤습니다</h3>
        <p>이제 로그인하면 소비 내역과 예산 현황을 확인할 수 있습니다.</p>
        <Link className="primary-button auth-action-link" href="/login">로그인하러 가기</Link>
      </div>
    );
  }

  return (
    <form className="login-form verification-form" onSubmit={confirm}>
      <label htmlFor="verificationCode">인증번호</label>
      <input
        id="verificationCode"
        name="verificationCode"
        className="verification-code-input"
        value={code}
        onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        placeholder="000000"
        aria-describedby="verification-help"
        disabled={pending !== null}
      />
      <p id="verification-help" className="field-help">인증번호는 발송 후 5분 동안 유효합니다.</p>
      {message && (
        <p className={error ? "form-error" : "form-notice"} role={error ? "alert" : "status"}>
          {message}
        </p>
      )}
      <button className="primary-button" type="submit" disabled={pending !== null}>
        {pending === "confirm" ? "확인 중..." : "인증 완료"}
      </button>
      <button
        className="resend-button"
        type="button"
        onClick={resend}
        disabled={pending !== null || remainingSeconds > 0}
      >
        {pending === "resend"
          ? "발송 중..."
          : remainingSeconds > 0
            ? `인증번호 다시 받기 (${remainingSeconds}초)`
            : "인증번호 다시 받기"}
      </button>
    </form>
  );
}
