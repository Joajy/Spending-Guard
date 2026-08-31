"use client";

import Link from "next/link";
import { FormEvent, useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import type { SpendEventDetail, SpendEventStatus } from "./types";

type AcceptedEvent = { eventId: string; status: SpendEventStatus; receivedAt: string };

export function SpendEventSubmissionView() {
  const router = useRouter();
  const pollTimer = useRef<number | null>(null);
  const [pending, setPending] = useState(false);
  const [accepted, setAccepted] = useState<AcceptedEvent | null>(null);
  const [detail, setDetail] = useState<SpendEventDetail | null>(null);
  const [message, setMessage] = useState("");
  const [statusMessage, setStatusMessage] = useState("");

  useEffect(() => () => {
    if (pollTimer.current !== null) window.clearTimeout(pollTimer.current);
  }, []);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const rawMessage = String(form.get("message") ?? "").trim();
    const occurredAt = String(form.get("occurredAt") ?? "");
    if (!rawMessage || rawMessage.length > 2_000) {
      setMessage("소비 알림은 1~2,000자로 입력해 주세요.");
      return;
    }

    setPending(true);
    setMessage("");
    setStatusMessage("");
    try {
      const response = await fetch("/api/transactions", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: rawMessage,
          occurredAt: occurredAt ? new Date(occurredAt).toISOString() : null,
        }),
      });
      if (response.status === 401) {
        router.replace("/login");
        router.refresh();
        return;
      }
      if (!response.ok) {
        const body = await response.json() as { message?: string };
        setMessage(body.message ?? "소비 알림을 접수하지 못했습니다.");
        return;
      }
      const receipt = await response.json() as AcceptedEvent;
      setAccepted(receipt);
      setDetail(null);
      setStatusMessage("소비 알림을 안전하게 접수했습니다. 분석 결과를 확인하고 있습니다.");
      await refreshStatus(receipt.eventId, 0);
    } catch {
      setMessage("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setPending(false);
    }
  }

  async function refreshStatus(eventId = accepted?.eventId, attempt = 0) {
    if (!eventId) return;
    try {
      const response = await fetch(`/api/transactions/${eventId}`, { cache: "no-store" });
      if (response.status === 401) {
        router.replace("/login");
        router.refresh();
        return;
      }
      if (!response.ok) {
        setStatusMessage("처리 상태를 불러오지 못했습니다. 잠시 후 다시 확인해 주세요.");
        return;
      }
      const current = await response.json() as SpendEventDetail;
      setDetail(current);
      if (!current.fastParse && current.status === "RECEIVED" && attempt < 4) {
        pollTimer.current = window.setTimeout(() => refreshStatus(eventId, attempt + 1), 2_000);
      } else if (!current.fastParse) {
        setStatusMessage("분석이 계속 진행 중입니다. 잠시 후 상태를 다시 확인해 주세요.");
      } else if (current.status === "NEEDS_REVIEW") {
        setStatusMessage("금액이나 거래 유형을 확정하지 못했습니다. 소비 내역에서 확인해 주세요.");
      } else {
        setStatusMessage("분석 결과를 확인했습니다.");
      }
    } catch {
      setStatusMessage("처리 상태를 불러오지 못했습니다. 접수된 알림은 보관되어 있습니다.");
    }
  }

  return (
    <div className="dashboard-shell">
      <header className="dashboard-header">
        <Link className="brand-lockup" href="/dashboard"><span className="brand-mark">SG</span><strong>Spending Guard</strong></Link>
        <nav className="header-actions" aria-label="주요 메뉴">
          <Link href="/dashboard">대시보드</Link><Link href="/transactions">소비 내역</Link><Link href="/alerts">위험 알림</Link>
        </nav>
      </header>
      <main className="dashboard-content ingestion-content">
        <div className="dashboard-title-row">
          <div><p className="eyebrow">ADD TRANSACTION</p><h1>소비 알림 등록</h1><p className="muted">카드나 은행에서 받은 알림을 붙여 넣으면 소비 정보와 위험 신호를 분석합니다.</p></div>
        </div>
        <div className="ingestion-layout">
          <form className="ingestion-form" onSubmit={submit}>
            <label htmlFor="spendMessage">금융 알림 텍스트</label>
            <textarea id="spendMessage" name="message" maxLength={2_000} placeholder="예: [카드 승인] 쿠팡 12,800원 결제" required />
            <p className="field-help">계좌번호 등 민감한 숫자는 저장 전에 일부 마스킹됩니다.</p>
            <label htmlFor="occurredAt">결제 발생 시각 <span>선택</span></label>
            <input id="occurredAt" name="occurredAt" type="datetime-local" />
            {message && <p className="form-error" role="alert">{message}</p>}
            <button className="primary-button ingestion-submit" type="submit" disabled={pending}>
              {pending ? "접수 중..." : "분석 요청하기"}
            </button>
          </form>
          <section className="ingestion-status" aria-live="polite" aria-label="처리 상태">
            {!accepted ? (
              <div className="ingestion-guide"><span aria-hidden="true">→</span><h2>알림을 등록해 주세요</h2><p>접수가 끝나면 원문 저장과 분석 진행 상태를 이곳에서 확인할 수 있습니다.</p></div>
            ) : (
              <div>
                <p className="eyebrow">PROCESSING STATUS</p>
                <h2>{detail?.fastParse ? "분석 결과" : "안전하게 접수됨"}</h2>
                <p className="muted">{statusMessage}</p>
                <dl className="ingestion-result">
                  <div><dt>처리 상태</dt><dd>{statusLabel(detail?.status ?? accepted.status)}</dd></div>
                  {detail?.fastParse?.amount != null && <div><dt>금액</dt><dd>{detail.fastParse.amount.toLocaleString("ko-KR")}원</dd></div>}
                  {detail?.fastParse?.category && <div><dt>카테고리</dt><dd>{detail.fastParse.category}</dd></div>}
                  {detail?.fastParse?.riskLevel && <div><dt>위험도</dt><dd>{detail.fastParse.riskLevel}</dd></div>}
                </dl>
                <button className="status-refresh" type="button" onClick={() => refreshStatus()}>처리 상태 다시 확인</button>
                <Link className="status-history-link" href="/transactions">전체 소비 내역 보기</Link>
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}

function statusLabel(status: SpendEventStatus): string {
  if (status === "RECEIVED") return "접수됨";
  if (status === "ANALYZING") return "분석 중";
  return "사용자 확인 필요";
}
