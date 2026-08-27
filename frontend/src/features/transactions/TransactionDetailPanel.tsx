"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import {
  CATEGORY_LABELS,
  formatFullDateTime,
  formatTransactionAmount,
  RISK_LABELS,
  reviewReasonLabel,
  riskReasonLabel,
  SOURCE_LABELS,
  STATUS_LABELS,
} from "./format";
import type {
  CategoryCorrectionResult,
  SpendCategory,
  SpendEventDetail,
  SpendEventHistoryItem,
} from "./types";

type Props = {
  item: SpendEventHistoryItem;
  onClose: () => void;
  onCorrected: (result: CategoryCorrectionResult) => void;
};

export function TransactionDetailPanel({ item, onClose, onCorrected }: Props) {
  const router = useRouter();
  const closeButton = useRef<HTMLButtonElement>(null);
  const [detail, setDetail] = useState<SpendEventDetail | null>(null);
  const [category, setCategory] = useState<SpendCategory>(item.category ?? "OTHER");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [saved, setSaved] = useState("");
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    closeButton.current?.focus();
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", closeOnEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", closeOnEscape);
    };
  }, [onClose]);

  useEffect(() => {
    const controller = new AbortController();
    let active = true;

    fetch(`/api/transactions/${item.eventId}`, { signal: controller.signal })
      .then(async (response) => {
        if (response.status === 401) {
          router.replace("/login");
          return null;
        }
        const body = await response.json() as SpendEventDetail | { message?: string };
        if (!response.ok) {
          throw new DetailRequestError(
            "message" in body ? body.message ?? "소비 상세를 불러오지 못했습니다." : "소비 상세를 불러오지 못했습니다.",
          );
        }
        return body as SpendEventDetail;
      })
      .then((body) => {
        if (!active || !body) return;
        setDetail(body);
        if (body.fastParse?.category) setCategory(body.fastParse.category);
      })
      .catch((cause: unknown) => {
        if (!active || isAbortError(cause)) return;
        setError(cause instanceof DetailRequestError
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
  }, [item.eventId, reloadKey, router]);

  async function correctCategory() {
    if (!detail?.fastParse || saving || category === detail.fastParse.category) return;
    setSaving(true);
    setSaveError("");
    setSaved("");
    try {
      const response = await fetch(`/api/transactions/${item.eventId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          category,
          expectedVersion: detail.fastParse.categoryVersion,
        }),
      });
      if (response.status === 401) {
        router.replace("/login");
        return;
      }
      const body = await response.json() as CategoryCorrectionResult | { message?: string };
      if (!response.ok) {
        setSaveError("message" in body ? body.message ?? "카테고리를 수정하지 못했습니다." : "카테고리를 수정하지 못했습니다.");
        return;
      }
      const result = body as CategoryCorrectionResult;
      setDetail((current) => current?.fastParse ? {
        ...current,
        fastParse: {
          ...current.fastParse,
          category: result.category,
          fixedCost: result.fixedCost,
          riskLevel: result.riskLevel,
          riskReason: result.riskReason,
          categoryVersion: result.version,
        },
      } : current);
      setCategory(result.category);
      setSaved(`${CATEGORY_LABELS[result.category]} 카테고리로 수정했습니다.`);
      onCorrected(result);
    } catch {
      setSaveError("네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
    } finally {
      setSaving(false);
    }
  }

  const parsed = detail?.fastParse;
  const editable = Boolean(parsed?.category && parsed.amount !== null && parsed.transactionType);

  function reloadDetail() {
    setLoading(true);
    setDetail(null);
    setError("");
    setSaveError("");
    setSaved("");
    setReloadKey((value) => value + 1);
  }

  return (
    <div className="detail-backdrop" role="presentation" onMouseDown={(event) => {
      if (event.target === event.currentTarget) onClose();
    }}>
      <aside className="transaction-detail" role="dialog" aria-modal="true" aria-labelledby="detail-title">
        <header className="detail-header">
          <div>
            <p className="eyebrow">TRANSACTION DETAIL</p>
            <h2 id="detail-title">소비 상세</h2>
          </div>
          <button ref={closeButton} className="detail-close" type="button" onClick={onClose} aria-label="상세 닫기">×</button>
        </header>

        <div className="detail-body">
          <section className="detail-summary">
            <div className={`transaction-icon ${item.riskLevel?.toLowerCase() ?? "pending"}`} aria-hidden="true" />
            <div>
              <strong>{item.displayText}</strong>
              <span>{formatTransactionAmount(item.amount, item.transactionType)}</span>
            </div>
          </section>

          {loading && <div className="detail-skeleton" aria-label="소비 상세 불러오는 중"><i /><i /><i /></div>}
          {!loading && error && (
            <section className="detail-message" role="alert">
              <strong>상세 정보를 불러오지 못했어요.</strong>
              <p>{error}</p>
              <button className="secondary-button" type="button" onClick={reloadDetail}>다시 시도</button>
            </section>
          )}
          {!loading && detail && (
            <>
              <section className="detail-section">
                <h3>거래 정보</h3>
                <dl className="detail-list">
                  <div><dt>처리 상태</dt><dd>{STATUS_LABELS[detail.status]}</dd></div>
                  <div><dt>유입 경로</dt><dd>{SOURCE_LABELS[detail.source]}</dd></div>
                  <div><dt>거래 시각</dt><dd>{formatFullDateTime(detail.occurredAt)}</dd></div>
                  <div><dt>접수 시각</dt><dd>{formatFullDateTime(detail.receivedAt)}</dd></div>
                </dl>
              </section>

              {parsed ? (
                <>
                  <section className="detail-section risk-explanation">
                    <h3>분석 결과</h3>
                    {parsed.status === "NEEDS_REVIEW" ? (
                      <p className="review-copy">{reviewReasonLabel(parsed.reviewReason)}</p>
                    ) : (
                      <>
                        <div className="detail-risk-row">
                          <span className={`risk-chip ${parsed.riskLevel?.toLowerCase() ?? "low"}`}>
                            {parsed.riskLevel ? RISK_LABELS[parsed.riskLevel] : "분석 중"}
                          </span>
                          {parsed.fixedCost && <span className="fixed-chip">고정비</span>}
                        </div>
                        <p>{riskReasonLabel(parsed.riskReason)}</p>
                      </>
                    )}
                  </section>

                  <section className="detail-section category-editor">
                    <div>
                      <h3>카테고리 수정</h3>
                      <p>잘못 분류된 카테고리를 바꾸면 예산과 위험도가 함께 다시 계산됩니다.</p>
                    </div>
                    <label>
                      <span>카테고리</span>
                      <select value={category} onChange={(event) => {
                        setCategory(event.target.value as SpendCategory);
                        setSaved("");
                        setSaveError("");
                      }} disabled={!editable || saving}>
                        {Object.entries(CATEGORY_LABELS).map(([value, label]) => (
                          <option key={value} value={value}>{label}</option>
                        ))}
                      </select>
                    </label>
                    {!editable && <p className="editor-note">분석이 완료된 내역만 수정할 수 있습니다.</p>}
                    {saveError && (
                      <div className="save-feedback error" role="alert">
                        <span>{saveError}</span>
                        <button type="button" onClick={reloadDetail}>최신 정보 불러오기</button>
                      </div>
                    )}
                    {saved && <p className="save-feedback success" role="status">{saved}</p>}
                    <button
                      className="primary-button detail-save"
                      type="button"
                      onClick={correctCategory}
                      disabled={!editable || saving || category === parsed.category}
                    >
                      {saving ? "수정하는 중..." : "카테고리 저장"}
                    </button>
                  </section>
                  <p className="analysis-version">분석 기준 {parsed.parserVersion}</p>
                </>
              ) : (
                <section className="detail-message"><strong>소비 내역을 분석하고 있습니다.</strong><p>잠시 후 다시 확인해 주세요.</p></section>
              )}
            </>
          )}
        </div>
      </aside>
    </div>
  );
}

function isAbortError(cause: unknown): boolean {
  return cause instanceof DOMException && cause.name === "AbortError";
}

class DetailRequestError extends Error {}
