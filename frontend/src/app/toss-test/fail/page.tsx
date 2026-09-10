import Link from "next/link";
import { AppHeader } from "@/features/navigation/AppHeader";

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function TossFailPage({ searchParams }: { searchParams: SearchParams }) {
  const query = await searchParams;
  const code = single(query.code) || "PAYMENT_FAILED";
  return (
    <div className="dashboard-shell">
      <AppHeader />
      <main className="dashboard-content toss-result-shell">
        <section className="toss-result-card failure">
          <span className="toss-result-icon" aria-hidden="true">!</span>
          <p className="eyebrow">TOSS TEST PAYMENT</p>
          <h1>테스트 결제가 완료되지 않았습니다</h1>
          <p className="muted">{safeMessage(single(query.message))}</p>
          <code>{code}</code>
          <Link className="primary-button" href="/toss-test">다시 테스트하기</Link>
        </section>
      </main>
    </div>
  );
}

function single(value: string | string[] | undefined): string {
  return typeof value === "string" ? value : "";
}

function safeMessage(message: string): string {
  return message.slice(0, 200) || "결제창이 닫혔거나 테스트 결제가 중단되었습니다.";
}
