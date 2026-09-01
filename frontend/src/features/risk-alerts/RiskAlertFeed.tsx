"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { monthLabel, moveMonth } from "@/features/dashboard/format";
import { AppHeader } from "@/features/navigation/AppHeader";
import {
  CATEGORY_LABELS,
  formatTransactionTime,
  RISK_LABELS,
  riskReasonLabel,
} from "@/features/transactions/format";
import { TransactionDetailPanel } from "@/features/transactions/TransactionDetailPanel";
import type {
  CategoryCorrectionResult,
  SpendEventHistoryItem,
} from "@/features/transactions/types";
import type { MinimumRiskLevel, RiskAlertItem, RiskAlertPage } from "./types";

type Props = { initialMonth: string };

export function RiskAlertFeed({ initialMonth }: Props) {
  const router = useRouter();
  const [month, setMonth] = useState(initialMonth);
  const [minimumLevel, setMinimumLevel] = useState<MinimumRiskLevel>("MEDIUM");
  const [items, setItems] = useState<RiskAlertItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [loadMoreError, setLoadMoreError] = useState("");
  const [notice, setNotice] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const [selectedItem, setSelectedItem] = useState<RiskAlertItem | null>(null);
  const queryVersion = useRef(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    fetch(alertUrl(month, minimumLevel), { signal: controller.signal })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/login");
          return null;
        }
        const body = await response.json() as RiskAlertPage | { message?: string };
        if (!response.ok) {
          throw new AlertRequestError(
            "message" in body ? body.message ?? "위험 알림을 불러오지 못했습니다." : "위험 알림을 불러오지 못했습니다.",
          );
        }
        return body as RiskAlertPage;
      })
      .then((page) => {
        if (!active || !page) return;
        setItems(page.items);
        setNextCursor(page.nextCursor);
        setHasNext(page.hasNext);
      })
      .catch((cause: unknown) => {
        if (!active || isAbortError(cause)) return;
        setError(cause instanceof AlertRequestError
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
  }, [month, minimumLevel, retryKey, router]);

  async function loadMore() {
    if (!nextCursor || loadingMore) return;
    const requestVersion = queryVersion.current;
    setLoadingMore(true);
    setLoadMoreError("");
    try {
      const response = await fetch(alertUrl(month, minimumLevel, nextCursor));
      if (response.status === 401) {
        router.replace("/login");
        return;
      }
      const body = await response.json() as RiskAlertPage | { message?: string };
      if (requestVersion !== queryVersion.current) return;
      if (!response.ok) {
        setLoadMoreError("message" in body ? body.message ?? "다음 알림을 불러오지 못했습니다." : "다음 알림을 불러오지 못했습니다.");
        return;
      }
      const page = body as RiskAlertPage;
      setItems((current) => [...current, ...page.items]);
      setNextCursor(page.nextCursor);
      setHasNext(page.hasNext);
    } catch {
      if (requestVersion === queryVersion.current) {
        setLoadMoreError("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
      }
    } finally {
      if (requestVersion === queryVersion.current) {
        setLoadingMore(false);
      }
    }
  }

  function prepareReload() {
    queryVersion.current += 1;
    setLoading(true);
    setLoadingMore(false);
    setError("");
    setLoadMoreError("");
    setNotice("");
  }

  function changeMonth(offset: number) {
    prepareReload();
    setMonth((current) => moveMonth(current, offset));
  }

  function changeMinimumLevel(value: MinimumRiskLevel) {
    prepareReload();
    setMinimumLevel(value);
  }

  function retry() {
    prepareReload();
    setRetryKey((value) => value + 1);
  }

  function closeDetail() {
    const eventId = selectedItem?.eventId;
    setSelectedItem(null);
    if (eventId) {
      requestAnimationFrame(() => document.getElementById(`alert-${eventId}`)?.focus());
    }
  }

  function handleCategoryCorrection(result: CategoryCorrectionResult) {
    const remainsVisible = minimumLevel === "MEDIUM"
      ? result.riskLevel !== "LOW"
      : result.riskLevel === "HIGH";
    if (!remainsVisible) {
      setItems((current) => current.filter((item) => item.eventId !== result.eventId));
      setSelectedItem(null);
      setNotice("다시 계산한 위험도가 낮아져 현재 알림 목록에서 제외되었습니다.");
      return;
    }
    setItems((current) => current.map((item) => item.eventId === result.eventId ? {
      ...item,
      category: result.category,
      riskLevel: result.riskLevel as RiskAlertItem["riskLevel"],
      reasonCode: result.riskReason,
      categoryVersion: result.version,
    } : item));
    setNotice("수정한 카테고리와 다시 계산된 위험도를 반영했습니다.");
  }

  return (
    <main className="dashboard-shell">
      <AppHeader />

      <section className="dashboard-content alert-content">
        <div className="dashboard-title-row">
          <div>
            <p className="eyebrow">RISK ALERTS</p>
            <h1>주의 소비 알림</h1>
            <p className="muted">평소보다 주의해서 확인할 소비와 판정 이유를 모아 보여드립니다.</p>
          </div>
          <div className="month-picker" aria-label="조회 월 변경">
            <button type="button" aria-label="이전 달" onClick={() => changeMonth(-1)}>‹</button>
            <strong>{monthLabel(month)}</strong>
            <button type="button" aria-label="다음 달" onClick={() => changeMonth(1)}>›</button>
          </div>
        </div>

        <section className="alert-toolbar" aria-label="위험 알림 필터">
          <div>
            <strong>표시할 위험도</strong>
            <span>높음만 보거나 주의 단계까지 함께 확인할 수 있습니다.</span>
          </div>
          <div className="risk-toggle">
            <button
              className={minimumLevel === "MEDIUM" ? "active" : ""}
              type="button"
              aria-pressed={minimumLevel === "MEDIUM"}
              onClick={() => changeMinimumLevel("MEDIUM")}
            >주의 이상</button>
            <button
              className={minimumLevel === "HIGH" ? "active" : ""}
              type="button"
              aria-pressed={minimumLevel === "HIGH"}
              onClick={() => changeMinimumLevel("HIGH")}
            >높음만</button>
          </div>
        </section>

        {notice && <p className="transaction-notice" role="status">{notice}</p>}
        {loading && <AlertSkeleton />}
        {!loading && error && (
          <section className="empty-state" role="alert">
            <h2>잠시 위험 알림을 불러오지 못했어요.</h2>
            <p>{error}</p>
            <button className="secondary-button" type="button" onClick={retry}>다시 시도</button>
          </section>
        )}
        {!loading && !error && items.length === 0 && (
          <section className="empty-state alert-empty">
            <span aria-hidden="true">✓</span>
            <h2>조건에 맞는 주의 소비가 없습니다.</h2>
            <p>현재 규칙에서 확인이 필요한 소비가 발견되지 않았습니다.</p>
          </section>
        )}
        {!loading && !error && items.length > 0 && (
          <section aria-label="위험 알림 목록">
            <ul className="alert-list">
              {items.map((item) => (
                <li key={item.eventId}>
                  <button
                    id={`alert-${item.eventId}`}
                    className={`alert-card ${item.riskLevel.toLowerCase()}`}
                    type="button"
                    onClick={() => {
                      setNotice("");
                      setSelectedItem(item);
                    }}
                    aria-label={`${item.displayText} 상세 보기`}
                  >
                    <span className="alert-severity">{RISK_LABELS[item.riskLevel]}</span>
                    <span className="alert-main">
                      <span className="alert-meta">{CATEGORY_LABELS[item.category]} · {formatTransactionTime(item.transactionAt)}</span>
                      <strong>{item.displayText}</strong>
                      <span className="alert-reason">{riskReasonLabel(item.reasonCode)}</span>
                    </span>
                    <strong className="alert-amount">{item.amount.toLocaleString("ko-KR")}원</strong>
                    <span className="transaction-chevron" aria-hidden="true">›</span>
                  </button>
                </li>
              ))}
            </ul>
            {loadMoreError && <p className="load-more-error" role="alert">{loadMoreError}</p>}
            {hasNext && (
              <button className="load-more-button" type="button" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? "불러오는 중..." : "위험 알림 더 보기"}
              </button>
            )}
          </section>
        )}
      </section>

      {selectedItem && (
        <TransactionDetailPanel
          key={selectedItem.eventId}
          item={asHistoryItem(selectedItem)}
          onClose={closeDetail}
          onCorrected={handleCategoryCorrection}
        />
      )}
    </main>
  );
}

function AlertSkeleton() {
  return <div className="alert-skeleton" aria-label="위험 알림 불러오는 중"><i /><i /><i /></div>;
}

function alertUrl(month: string, minimumLevel: MinimumRiskLevel, cursor?: string): string {
  const query = new URLSearchParams({ month, minimumLevel });
  if (cursor) query.set("cursor", cursor);
  return `/api/risk-alerts?${query}`;
}

function asHistoryItem(item: RiskAlertItem): SpendEventHistoryItem {
  return {
    eventId: item.eventId,
    displayText: item.displayText,
    status: "ANALYZING",
    transactionAt: item.transactionAt,
    amount: item.amount,
    currency: "KRW",
    transactionType: "PAYMENT",
    category: item.category,
    fixedCost: null,
    riskLevel: item.riskLevel,
    categoryVersion: item.categoryVersion,
  };
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof DOMException && cause.name === "AbortError";
}

class AlertRequestError extends Error {}
