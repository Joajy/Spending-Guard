"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import type { MonthlyDashboard } from "./types";
import { budgetProgress, formatWon, monthLabel, moveMonth } from "./format";

type Props = { initialMonth: string };

export function DashboardView({ initialMonth }: Props) {
  const router = useRouter();
  const [month, setMonth] = useState(initialMonth);
  const [dashboard, setDashboard] = useState<MonthlyDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    fetch(`/api/dashboard?month=${month}`, { signal: controller.signal })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/login");
          return;
        }
        const body = await response.json() as MonthlyDashboard | { message?: string };
        if (!response.ok) {
          setError("message" in body ? body.message ?? "데이터를 불러오지 못했습니다." : "데이터를 불러오지 못했습니다.");
          return;
        }
        setDashboard(body as MonthlyDashboard);
      })
      .catch((cause: unknown) => {
        if (!(cause instanceof DOMException && cause.name === "AbortError")) {
          setError("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
        }
      })
      .finally(() => setLoading(false));
    return () => controller.abort();
  }, [month, retryKey, router]);

  async function logout() {
    await fetch("/api/session", { method: "DELETE" });
    router.replace("/login");
    router.refresh();
  }

  function changeMonth(offset: number) {
    setLoading(true);
    setError("");
    setMonth((current) => moveMonth(current, offset));
  }

  function retry() {
    setLoading(true);
    setError("");
    setRetryKey((current) => current + 1);
  }

  const maxCategory = useMemo(
    () => Math.max(1, ...(dashboard?.categories.map((item) => item.amount) ?? [])),
    [dashboard],
  );

  const risk = (level: "LOW" | "MEDIUM" | "HIGH") =>
    dashboard?.risks.find((item) => item.riskLevel === level)?.count ?? 0;

  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <div className="brand-lockup"><span className="brand-mark">SG</span><strong>Spending Guard</strong></div>
        <nav className="header-actions" aria-label="주요 메뉴">
          <Link href="/budget">예산</Link>
          <Link href="/alerts">위험 알림</Link>
          <Link href="/transactions">소비 내역</Link>
          <button className="text-button" type="button" onClick={logout}>로그아웃</button>
        </nav>
      </header>

      <section className="dashboard-content">
        <div className="dashboard-title-row">
          <div>
            <p className="eyebrow">MONTHLY OVERVIEW</p>
            <h1>이번 달 소비 흐름</h1>
            <p className="muted">예산 안에서 소비하고 있는지, 주의할 지출은 없는지 확인해 보세요.</p>
          </div>
          <div className="month-picker" aria-label="조회 월 변경">
            <button type="button" aria-label="이전 달" onClick={() => changeMonth(-1)}>‹</button>
            <strong>{monthLabel(month)}</strong>
            <button type="button" aria-label="다음 달" onClick={() => changeMonth(1)}>›</button>
          </div>
        </div>

        {loading && <DashboardSkeleton />}
        {!loading && error && (
          <section className="empty-state" role="alert">
            <h2>잠시 데이터를 불러오지 못했어요.</h2>
            <p>{error}</p>
            <button className="secondary-button" type="button" onClick={retry}>다시 시도</button>
          </section>
        )}
        {!loading && !error && dashboard && (
          <>
            <section className="summary-grid" aria-label="월간 소비 요약">
              {dashboard.budget ? (
                <article className="summary-card budget-card">
                  <span>남은 예산</span>
                  <strong>{formatWon(dashboard.budget.remainingAmount)}</strong>
                  <div className="progress-track" aria-label={`예산 ${budgetProgress(dashboard.budget.spentAmount, dashboard.budget.limitAmount)}% 사용`}>
                    <i style={{ width: `${budgetProgress(dashboard.budget.spentAmount, dashboard.budget.limitAmount)}%` }} />
                  </div>
                  <small>{formatWon(dashboard.budget.limitAmount)} 중 {formatWon(dashboard.budget.spentAmount)} 사용</small>
                </article>
              ) : (
                <article className="summary-card budget-card budget-card-empty">
                  <span>월간 예산</span>
                  <strong>예산을 설정해 주세요.</strong>
                  <p>한도를 정하면 남은 금액과 소비 속도를 바로 확인할 수 있습니다.</p>
                  <Link href={`/budget?month=${month}`}>예산 설정하기 →</Link>
                </article>
              )}
              <article className="summary-card">
                <span>총 지출</span>
                <strong>{formatWon(dashboard.totalSpending)}</strong>
                <small>{dashboard.transactionCount.toLocaleString("ko-KR")}건의 소비 내역</small>
              </article>
              <article className="summary-card risk-card">
                <span>주의가 필요한 소비</span>
                <strong>{risk("HIGH") + risk("MEDIUM")}건</strong>
                <small>높음 {risk("HIGH")} · 주의 {risk("MEDIUM")}</small>
              </article>
            </section>

            <section className="detail-grid">
              <article className="panel category-panel">
                <div className="panel-heading"><div><span>카테고리별 지출</span><h2>어디에 가장 많이 썼을까요?</h2></div></div>
                {dashboard.categories.length === 0 ? <p className="panel-empty">아직 집계된 소비가 없습니다.</p> : (
                  <ul className="category-list">
                    {dashboard.categories.map((item) => (
                      <li key={item.category}>
                        <div><strong>{item.category}</strong><span>{item.transactionCount}건 · {formatWon(item.amount)}</span></div>
                        <div className="category-track"><i style={{ width: `${Math.round((item.amount / maxCategory) * 100)}%` }} /></div>
                      </li>
                    ))}
                  </ul>
                )}
              </article>
              <article className="panel risk-panel">
                <div className="panel-heading"><div><span>위험도 분포</span><h2>이번 달 소비 신호</h2></div></div>
                <div className="risk-list">
                  <RiskRow label="높음" count={risk("HIGH")} tone="high" />
                  <RiskRow label="주의" count={risk("MEDIUM")} tone="medium" />
                  <RiskRow label="안정" count={risk("LOW")} tone="low" />
                </div>
                <p className="risk-help">심야 교통비나 반복되는 고액 지출처럼 평소와 다른 패턴을 우선 보여드립니다.</p>
              </article>
            </section>
          </>
        )}
      </section>
    </main>
  );
}

function RiskRow({ label, count, tone }: { label: string; count: number; tone: string }) {
  return <div className={`risk-row ${tone}`}><span><i />{label}</span><strong>{count}건</strong></div>;
}

function DashboardSkeleton() {
  return <div className="dashboard-skeleton" aria-label="대시보드 불러오는 중"><i /><i /><i /><i /><i /></div>;
}
