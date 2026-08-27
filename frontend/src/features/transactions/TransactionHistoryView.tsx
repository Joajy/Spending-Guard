"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { monthLabel, moveMonth } from "@/features/dashboard/format";
import {
  CATEGORY_LABELS,
  formatTransactionAmount,
  formatTransactionTime,
  RISK_LABELS,
  STATUS_LABELS,
} from "./format";
import type {
  SpendCategory,
  SpendEventHistoryItem,
  SpendEventHistoryPage,
  SpendEventStatus,
} from "./types";

type Props = { initialMonth: string };
type StatusFilter = SpendEventStatus | "";
type CategoryFilter = SpendCategory | "";

export function TransactionHistoryView({ initialMonth }: Props) {
  const router = useRouter();
  const [month, setMonth] = useState(initialMonth);
  const [status, setStatus] = useState<StatusFilter>("");
  const [category, setCategory] = useState<CategoryFilter>("");
  const [items, setItems] = useState<SpendEventHistoryItem[]>([]);
  const [nextCursor, setNextCursor] = useState<string | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [loadMoreError, setLoadMoreError] = useState("");
  const [retryKey, setRetryKey] = useState(0);
  const queryVersion = useRef(0);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    fetch(historyUrl(month, status, category), { signal: controller.signal })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/login");
          return null;
        }
        const body = await response.json() as SpendEventHistoryPage | { message?: string };
        if (!response.ok) {
          throw new HistoryRequestError(
            "message" in body ? body.message ?? "소비 내역을 불러오지 못했습니다." : "소비 내역을 불러오지 못했습니다.",
          );
        }
        return body as SpendEventHistoryPage;
      })
      .then((page) => {
        if (!active || !page) return;
        setItems(page.items);
        setNextCursor(page.nextCursor);
        setHasNext(page.hasNext);
      })
      .catch((cause: unknown) => {
        if (!active || isAbortError(cause)) return;
        setError(cause instanceof HistoryRequestError
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
  }, [month, status, category, retryKey, router]);

  async function loadMore() {
    if (!nextCursor || loadingMore) return;
    const requestVersion = queryVersion.current;
    setLoadingMore(true);
    setLoadMoreError("");
    try {
      const response = await fetch(historyUrl(month, status, category, nextCursor));
      if (response.status === 401) {
        router.replace("/login");
        return;
      }
      const body = await response.json() as SpendEventHistoryPage | { message?: string };
      if (requestVersion !== queryVersion.current) return;
      if (!response.ok) {
        setLoadMoreError("message" in body ? body.message ?? "다음 내역을 불러오지 못했습니다." : "다음 내역을 불러오지 못했습니다.");
        return;
      }
      const page = body as SpendEventHistoryPage;
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

  async function logout() {
    await fetch("/api/session", { method: "DELETE" });
    router.replace("/login");
    router.refresh();
  }

  function changeMonth(offset: number) {
    prepareReload();
    setMonth((current) => moveMonth(current, offset));
  }

  function changeStatus(value: StatusFilter) {
    prepareReload();
    setStatus(value);
  }

  function changeCategory(value: CategoryFilter) {
    prepareReload();
    setCategory(value);
  }

  function retry() {
    prepareReload();
    setRetryKey((value) => value + 1);
  }

  function prepareReload() {
    queryVersion.current += 1;
    setLoading(true);
    setLoadingMore(false);
    setError("");
    setLoadMoreError("");
  }

  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <Link className="brand-lockup" href="/dashboard">
          <span className="brand-mark">SG</span><strong>Spending Guard</strong>
        </Link>
        <nav className="header-actions" aria-label="주요 메뉴">
          <Link href="/dashboard">대시보드</Link>
          <button className="text-button" type="button" onClick={logout}>로그아웃</button>
        </nav>
      </header>

      <section className="dashboard-content transaction-content">
        <div className="dashboard-title-row">
          <div>
            <p className="eyebrow">TRANSACTION HISTORY</p>
            <h1>소비 내역</h1>
            <p className="muted">정리된 소비와 확인이 필요한 알림을 월별로 살펴보세요.</p>
          </div>
          <div className="month-picker" aria-label="조회 월 변경">
            <button type="button" aria-label="이전 달" onClick={() => changeMonth(-1)}>‹</button>
            <strong>{monthLabel(month)}</strong>
            <button type="button" aria-label="다음 달" onClick={() => changeMonth(1)}>›</button>
          </div>
        </div>

        <section className="transaction-filters" aria-label="소비 내역 필터">
          <label>
            <span>처리 상태</span>
            <select value={status} onChange={(event) => changeStatus(event.target.value as StatusFilter)}>
              <option value="">전체 상태</option>
              <option value="RECEIVED">접수됨</option>
              <option value="ANALYZING">분석 중</option>
              <option value="NEEDS_REVIEW">확인 필요</option>
            </select>
          </label>
          <label>
            <span>카테고리</span>
            <select value={category} onChange={(event) => changeCategory(event.target.value as CategoryFilter)}>
              <option value="">전체 카테고리</option>
              {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                <option key={value} value={value}>{label}</option>
              ))}
            </select>
          </label>
        </section>

        {loading && <TransactionSkeleton />}
        {!loading && error && (
          <section className="empty-state" role="alert">
            <h2>잠시 소비 내역을 불러오지 못했어요.</h2>
            <p>{error}</p>
            <button className="secondary-button" type="button" onClick={retry}>다시 시도</button>
          </section>
        )}
        {!loading && !error && items.length === 0 && (
          <section className="empty-state">
            <h2>조건에 맞는 소비 내역이 없습니다.</h2>
            <p>조회 월이나 필터를 바꿔 확인해 보세요.</p>
          </section>
        )}
        {!loading && !error && items.length > 0 && (
          <section className="transaction-panel" aria-label="소비 내역 목록">
            <ul className="transaction-list">
              {items.map((item) => <TransactionRow key={item.eventId} item={item} />)}
            </ul>
            {loadMoreError && <p className="load-more-error" role="alert">{loadMoreError}</p>}
            {hasNext && (
              <button className="load-more-button" type="button" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? "불러오는 중..." : "소비 내역 더 보기"}
              </button>
            )}
          </section>
        )}
      </section>
    </main>
  );
}

function TransactionRow({ item }: { item: SpendEventHistoryItem }) {
  return (
    <li className="transaction-row">
      <div className={`transaction-icon ${item.riskLevel?.toLowerCase() ?? "pending"}`} aria-hidden="true" />
      <div className="transaction-main">
        <div className="transaction-labels">
          <span className={`status-chip ${item.status.toLowerCase()}`}>{STATUS_LABELS[item.status]}</span>
          {item.category && <span>{CATEGORY_LABELS[item.category]}</span>}
          {item.fixedCost && <span>고정비</span>}
          {item.riskLevel && item.riskLevel !== "LOW" && (
            <span className={`risk-chip ${item.riskLevel.toLowerCase()}`}>{RISK_LABELS[item.riskLevel]}</span>
          )}
        </div>
        <strong>{item.displayText}</strong>
        <small>{formatTransactionTime(item.transactionAt)}</small>
      </div>
      <strong className={`transaction-amount ${item.transactionType === "PAYMENT" ? "payment" : "credit"}`}>
        {formatTransactionAmount(item.amount, item.transactionType)}
      </strong>
    </li>
  );
}

function TransactionSkeleton() {
  return <div className="transaction-skeleton" aria-label="소비 내역 불러오는 중"><i /><i /><i /><i /></div>;
}

function historyUrl(
  month: string,
  status: StatusFilter,
  category: CategoryFilter,
  cursor?: string,
): string {
  const query = new URLSearchParams({ month });
  if (status) query.set("status", status);
  if (category) query.set("category", category);
  if (cursor) query.set("cursor", cursor);
  return `/api/transactions?${query}`;
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof DOMException && cause.name === "AbortError";
}

class HistoryRequestError extends Error {}
