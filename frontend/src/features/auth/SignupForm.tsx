"use client";

import Link from "next/link";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export function SignupForm() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const email = String(form.get("email") ?? "").trim();
    const password = String(form.get("password") ?? "");
    const passwordConfirmation = String(form.get("passwordConfirmation") ?? "");

    if (password.length < 8 || password.length > 72) {
      setMessage("비밀번호는 8~72자로 입력해 주세요.");
      return;
    }
    if (password !== passwordConfirmation) {
      setMessage("비밀번호가 서로 일치하지 않습니다.");
      return;
    }

    setPending(true);
    setMessage("");
    try {
      const registration = await fetch("/api/registration", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
      });

      if (!registration.ok) {
        const body = await registration.json() as { message?: string };
        setMessage(body.message ?? "회원가입을 완료하지 못했습니다.");
        return;
      }

      let verificationSent = false;
      try {
        const verification = await fetch("/api/email-verification", { method: "POST" });
        verificationSent = verification.ok;
      } catch {
        // The account already exists, so code delivery is retried from the verification step.
      }
      router.replace(verificationSent ? "/verify-email?sent=1" : "/verify-email?sent=0");
      router.refresh();
    } catch {
      setMessage("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setPending(false);
    }
  }

  return (
    <>
      <form className="login-form" onSubmit={submit}>
        <label htmlFor="email">이메일</label>
        <input
          id="email"
          name="email"
          type="email"
          autoComplete="email"
          placeholder="name@example.com"
          maxLength={320}
          required
        />
        <label htmlFor="password">비밀번호</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="new-password"
          placeholder="8~72자로 입력해 주세요"
          minLength={8}
          maxLength={72}
          required
        />
        <label htmlFor="passwordConfirmation">비밀번호 확인</label>
        <input
          id="passwordConfirmation"
          name="passwordConfirmation"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 한 번 더 입력해 주세요"
          minLength={8}
          maxLength={72}
          required
        />
        {message && <p className="form-error" role="alert">{message}</p>}
        <button className="primary-button" type="submit" disabled={pending}>
          {pending ? "계정 만드는 중..." : "계정 만들기"}
        </button>
      </form>
      <p className="auth-link-row">
        이미 계정이 있나요? <Link href="/login">로그인</Link>
      </p>
    </>
  );
}
