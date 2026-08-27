"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";

export function LoginForm() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setMessage("");

    const form = new FormData(event.currentTarget);
    try {
      const response = await fetch("/api/session", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          email: form.get("email"),
          password: form.get("password"),
        }),
      });

      if (!response.ok) {
        const body = await response.json() as { message?: string };
        setMessage(body.message ?? "로그인하지 못했습니다.");
        return;
      }

      router.replace("/dashboard");
      router.refresh();
    } catch {
      setMessage("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setPending(false);
    }
  }

  return (
    <form className="login-form" onSubmit={submit}>
      <label htmlFor="email">이메일</label>
      <input
        id="email"
        name="email"
        type="email"
        autoComplete="email"
        placeholder="name@example.com"
        required
      />
      <label htmlFor="password">비밀번호</label>
      <input
        id="password"
        name="password"
        type="password"
        autoComplete="current-password"
        placeholder="비밀번호를 입력해 주세요"
        required
      />
      {message && <p className="form-error" role="alert">{message}</p>}
      <button className="primary-button" type="submit" disabled={pending}>
        {pending ? "확인 중..." : "로그인"}
      </button>
    </form>
  );
}
