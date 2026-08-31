import { SignupForm } from "@/features/auth/SignupForm";

export default function RegisterPage() {
  return (
    <main className="login-shell">
      <section className="login-intro" aria-labelledby="intro-title">
        <span className="brand-mark" aria-hidden="true">SG</span>
        <p className="eyebrow">START WITH A PLAN</p>
        <h1 id="intro-title">소비 기록 전에<br />기준을 세워보세요.</h1>
        <p className="intro-copy">
          이메일 인증을 마치면 월간 예산과 위험 소비 알림을 한곳에서 관리할 수 있습니다.
        </p>
        <div className="signal-card onboarding-signal" aria-hidden="true">
          <div><span>가입 절차</span><strong>3단계</strong></div>
          <ol><li>계정 생성</li><li>이메일 인증</li><li>예산 설정</li></ol>
        </div>
      </section>
      <section className="login-panel" aria-labelledby="register-title">
        <div className="login-card">
          <p className="eyebrow">CREATE ACCOUNT</p>
          <h2 id="register-title">회원가입</h2>
          <p className="muted">서비스 알림을 받을 이메일과 안전한 비밀번호를 입력해 주세요.</p>
          <SignupForm />
          <p className="security-note">
            비밀번호는 암호화되어 저장되며 이메일은 가입 확인과 중요 알림에만 사용됩니다.
          </p>
        </div>
      </section>
    </main>
  );
}
