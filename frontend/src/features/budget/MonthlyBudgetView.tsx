"use client";

import { FormEvent, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { budgetProgress, formatWon, monthLabel, moveMonth } from "@/features/dashboard/format";
import { AppHeader } from "@/features/navigation/AppHeader";
import { formatFullDateTime } from "@/features/transactions/format";
import { formatBudgetInput, parseBudgetAmount } from "./format";
import type { MonthlyBudget } from "./types";

type Props = { initialMonth: string };

const PRESETS = [500_000, 1_000_000, 1_500_000];

export function MonthlyBudgetView({ initialMonth }: Props) {
  const router = useRouter();
  const [month, setMonth] = useState(initialMonth);
  const [budget, setBudget] = useState<MonthlyBudget | null>(null);
  const [amountInput, setAmountInput] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [formError, setFormError] = useState("");
  const [saveError, setSaveError] = useState("");
  const [saved, setSaved] = useState("");
  const [conflict, setConflict] = useState(false);
  const [retryKey, setRetryKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    fetch(`/api/budget?month=${month}`, { signal: controller.signal })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/login");
          return null;
        }
        if (response.status === 404) {
          return { missing: true } as const;
        }
        const body = await response.json() as MonthlyBudget | { message?: string };
        if (!response.ok) {
          throw new BudgetRequestError(
            "message" in body ? body.message ?? "예산을 불러오지 못했습니다." : "예산을 불러오지 못했습니다.",
          );
        }
        return body as MonthlyBudget;
      })
      .then((result) => {
        if (!active || !result) return;
        if ("missing" in result) {
          setBudget(null);
          setAmountInput("");
          return;
        }
        setBudget(result);
        setAmountInput(formatBudgetInput(String(result.limitAmount)));
      })
      .catch((cause: unknown) => {
        if (!active || isAbortError(cause)) return;
        setError(cause instanceof BudgetRequestError
          ? cause.message
          : "네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
      controller.abort();
    };
  }, [month, retryKey, router]);

  async function saveBudget(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const amount = parseBudgetAmount(amountInput);
    if (amount === null) {
      setFormError("1원 이상 1조 원 이하의 금액을 입력해 주세요.");
      return;
    }
    if (saving || amount === budget?.limitAmount) return;

    const wasCreated = budget === null;
    setSaving(true);
    setFormError("");
    setSaveError("");
    setSaved("");
    setConflict(false);
    try {
      const response = await fetch(`/api/budget?month=${month}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount, version: budget?.version ?? null }),
      });
      if (response.status === 401) {
        router.replace("/login");
        return;
      }
      if (response.status === 409) {
        setConflict(true);
        setSaveError("다른 화면에서 예산이 먼저 변경되었습니다. 최신 값을 다시 불러와 주세요.");
        return;
      }
      const body = await response.json() as MonthlyBudget | { message?: string };
      if (!response.ok) {
        setSaveError("message" in body ? body.message ?? "예산을 저장하지 못했습니다." : "예산을 저장하지 못했습니다.");
        return;
      }
      const savedBudget = body as MonthlyBudget;
      setBudget(savedBudget);
      setAmountInput(formatBudgetInput(String(savedBudget.limitAmount)));
      setSaved(wasCreated ? "이번 달 예산을 설정했습니다." : "이번 달 예산을 수정했습니다.");
    } catch {
      setSaveError("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  function prepareReload() {
    setLoading(true);
    setError("");
    setFormError("");
    setSaveError("");
    setSaved("");
    setConflict(false);
  }

  function changeMonth(offset: number) {
    prepareReload();
    setMonth((current) => moveMonth(current, offset));
  }

  function reloadBudget() {
    prepareReload();
    setRetryKey((current) => current + 1);
  }

  const parsedAmount = parseBudgetAmount(amountInput);
  const unchanged = parsedAmount !== null && parsedAmount === budget?.limitAmount;
  const belowSpent = parsedAmount !== null && budget !== null && parsedAmount < budget.spentAmount;

  return (
    <main className="dashboard-shell">
      <AppHeader />

      <section className="dashboard-content budget-content">
        <div className="dashboard-title-row">
          <div>
            <p className="eyebrow">MONTHLY BUDGET</p>
            <h1>월간 예산</h1>
            <p className="muted">한 달에 사용할 금액을 정하고 현재 소비 속도를 확인해 보세요.</p>
          </div>
          <div className="month-picker" aria-label="조회 월 변경">
            <button type="button" aria-label="이전 달" onClick={() => changeMonth(-1)}>‹</button>
            <strong>{monthLabel(month)}</strong>
            <button type="button" aria-label="다음 달" onClick={() => changeMonth(1)}>›</button>
          </div>
        </div>

        {loading && <BudgetSkeleton />}
        {!loading && error && (
          <section className="empty-state" role="alert">
            <h2>잠시 예산을 불러오지 못했어요.</h2>
            <p>{error}</p>
            <button className="secondary-button" type="button" onClick={reloadBudget}>다시 시도</button>
          </section>
        )}
        {!loading && !error && (
          <>
            {budget ? <BudgetOverview budget={budget} /> : (
              <section className="budget-empty-banner">
                <span aria-hidden="true">₩</span>
                <div><strong>아직 설정한 예산이 없습니다.</strong><p>금액을 저장하면 소비할 때마다 남은 예산이 자동으로 계산됩니다.</p></div>
              </section>
            )}

            <form className="budget-editor" onSubmit={saveBudget} noValidate>
              <div className="budget-editor-heading">
                <div>
                  <span>{budget ? "예산 수정" : "첫 예산 설정"}</span>
                  <h2>{budget ? "사용 계획이 달라졌나요?" : "이번 달에는 얼마를 쓸까요?"}</h2>
                </div>
                {budget && <small>마지막 변경 {formatFullDateTime(budget.updatedAt)}</small>}
              </div>
              <label htmlFor="budget-amount">월간 예산</label>
              <div className="budget-input-wrap">
                <input
                  id="budget-amount"
                  inputMode="numeric"
                  autoComplete="off"
                  placeholder="예: 1,000,000"
                  value={amountInput}
                  onChange={(event) => {
                    setAmountInput(formatBudgetInput(event.target.value));
                    setFormError("");
                    setSaveError("");
                    setSaved("");
                    setConflict(false);
                  }}
                  aria-describedby="budget-help"
                />
                <span>원</span>
              </div>
              <p id="budget-help" className="budget-help">1원부터 1조 원까지 설정할 수 있습니다.</p>
              <div className="budget-presets" aria-label="추천 예산 금액">
                {PRESETS.map((preset) => (
                  <button key={preset} type="button" onClick={() => {
                    setAmountInput(formatBudgetInput(String(preset)));
                    setFormError("");
                    setSaveError("");
                    setSaved("");
                    setConflict(false);
                  }}>{preset / 10_000}만원</button>
                ))}
              </div>

              {belowSpent && <p className="budget-warning">현재 지출보다 적은 예산입니다. 저장하면 남은 예산이 초과 상태로 표시됩니다.</p>}
              {formError && <p className="save-feedback error" role="alert">{formError}</p>}
              {saveError && (
                <div className="save-feedback error" role="alert">
                  <span>{saveError}</span>
                  {conflict && <button type="button" onClick={reloadBudget}>최신 예산 불러오기</button>}
                </div>
              )}
              {saved && <p className="save-feedback success" role="status">{saved}</p>}
              <button className="primary-button budget-save" type="submit" disabled={saving || parsedAmount === null || unchanged}>
                {saving ? "저장하는 중..." : budget ? "예산 수정" : "예산 설정"}
              </button>
            </form>
          </>
        )}
      </section>
    </main>
  );
}

function BudgetOverview({ budget }: { budget: MonthlyBudget }) {
  const progress = budgetProgress(budget.spentAmount, budget.limitAmount);
  return (
    <section className="budget-overview" aria-label="월간 예산 현황">
      <article className="budget-overview-primary">
        <span>남은 예산</span>
        <strong className={budget.remainingAmount < 0 ? "negative" : ""}>{formatWon(budget.remainingAmount)}</strong>
        <div className="budget-progress" aria-label={`예산 ${progress}% 사용`}><i style={{ width: `${progress}%` }} /></div>
        <small>{formatWon(budget.limitAmount)} 중 {formatWon(budget.spentAmount)} 사용</small>
      </article>
      <article><span>설정 예산</span><strong>{formatWon(budget.limitAmount)}</strong><small>이번 달 사용 한도</small></article>
      <article><span>현재 지출</span><strong>{formatWon(budget.spentAmount)}</strong><small>{progress}% 사용했습니다.</small></article>
    </section>
  );
}

function BudgetSkeleton() {
  return <div className="budget-skeleton" aria-label="월간 예산 불러오는 중"><i /><i /><i /></div>;
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof DOMException && cause.name === "AbortError";
}

class BudgetRequestError extends Error {}
