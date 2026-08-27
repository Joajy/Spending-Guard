import { LoginForm } from "@/features/auth/LoginForm";

export default function LoginPage() {
  return (
    <main className="login-shell">
      <section className="login-intro" aria-labelledby="intro-title">
        <span className="brand-mark" aria-hidden="true">SG</span>
        <p className="eyebrow">SPENDING GUARD</p>
        <h1 id="intro-title">놓치기 쉬운 소비 신호를<br />먼저 발견하세요.</h1>
        <p className="intro-copy">
          흩어진 소비 내역을 모아 이번 달 예산과 위험 지출을 한눈에 보여드립니다.
        </p>
        <div className="signal-card" aria-hidden="true">
          <div>
            <span>이번 달 위험 신호</span>
            <strong>3건</strong>
          </div>
          <div className="signal-bars">
            <i /><i /><i /><i /><i />
          </div>
        </div>
      </section>
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-card">
          <p className="eyebrow">WELCOME BACK</p>
          <h2 id="login-title">로그인</h2>
          <p className="muted">인증을 마친 계정으로 내 소비 현황을 확인하세요.</p>
          <LoginForm />
          <p className="security-note">
            로그인 정보는 브라우저 스크립트에 노출되지 않도록 안전한 쿠키로 관리됩니다.
          </p>
        </div>
      </section>
    </main>
  );
}
