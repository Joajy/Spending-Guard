import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { VerifyEmailForm } from "@/features/auth/VerifyEmailForm";
import { ONBOARDING_COOKIE } from "@/lib/server/onboarding";

type VerifyEmailPageProps = { searchParams: Promise<{ sent?: string }> };

export default async function VerifyEmailPage({ searchParams }: VerifyEmailPageProps) {
  const cookieStore = await cookies();
  if (!cookieStore.has(ONBOARDING_COOKIE)) redirect("/register");
  const { sent } = await searchParams;

  return (
    <main className="login-shell">
      <section className="login-intro" aria-labelledby="intro-title">
        <span className="brand-mark" aria-hidden="true">SG</span>
        <p className="eyebrow">VERIFY YOUR EMAIL</p>
        <h1 id="intro-title">마지막 한 단계만<br />확인해 주세요.</h1>
        <p className="intro-copy">
          계정 도용을 막고 중요한 소비 경고를 정확한 사용자에게 전달하기 위한 절차입니다.
        </p>
        <div className="signal-card verification-signal" aria-hidden="true">
          <div><span>인증번호 유효 시간</span><strong>05:00</strong></div>
          <div className="verification-dots"><i /><i /><i /><i /><i /><i /></div>
        </div>
      </section>
      <section className="login-panel" aria-labelledby="verification-title">
        <div className="login-card">
          <p className="eyebrow">EMAIL VERIFICATION</p>
          <h2 id="verification-title">이메일 인증</h2>
          <p className="muted">메일함에 도착한 6자리 인증번호를 입력해 주세요.</p>
          <VerifyEmailForm initiallySent={sent === "1"} />
        </div>
      </section>
    </main>
  );
}
