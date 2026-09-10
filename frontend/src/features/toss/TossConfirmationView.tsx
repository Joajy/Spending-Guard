"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { AppHeader } from "@/features/navigation/AppHeader";

type State = "confirming" | "success" | "failure";

export function TossConfirmationView(props: { paymentKey: string; orderId: string; amount: string }) {
  const amount = Number(props.amount);
  const invalidRedirect = !props.paymentKey || !props.orderId || !Number.isSafeInteger(amount);
  const started = useRef(false);
  const [state, setState] = useState<State>(invalidRedirect ? "failure" : "confirming");
  const [message, setMessage] = useState(
    invalidRedirect
      ? "Toss가 전달한 결제 승인 정보가 올바르지 않습니다."
      : "Toss 승인 결과와 서버 주문을 대조하고 있습니다.",
  );
  const [canceling, setCanceling] = useState(false);
  const [canceled, setCanceled] = useState(false);

  useEffect(() => {
    if (invalidRedirect || started.current) return;
    started.current = true;
    fetch(`/api/toss-test/orders/${encodeURIComponent(props.orderId)}/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ paymentKey: props.paymentKey, amount }),
    })
      .then(async (response) => {
        if (!response.ok) {
          const body = await response.json() as { message?: string };
          throw new Error(body.message ?? "결제 승인을 완료하지 못했습니다.");
        }
        setState("success");
        setMessage("결제가 접수됐습니다. Kafka 분석 후 소비 내역과 예산에 자동 반영됩니다.");
      })
      .catch((error: unknown) => {
        setState("failure");
        setMessage(error instanceof Error ? error.message : "결제 승인을 완료하지 못했습니다.");
      });
  }, [amount, invalidRedirect, props.orderId, props.paymentKey]);

  async function cancelPayment() {
    if (canceling || canceled) return;
    setCanceling(true);
    try {
      const response = await fetch(
        `/api/toss-test/orders/${encodeURIComponent(props.orderId)}/cancel`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ paymentKey: props.paymentKey }),
        },
      );
      if (!response.ok) {
        const body = await response.json() as { message?: string };
        throw new Error(body.message ?? "테스트 결제를 취소하지 못했습니다.");
      }
      setCanceled(true);
      setMessage("전체 취소가 접수됐습니다. 취소 내역과 예산 감소도 자동 반영됩니다.");
    } catch (error: unknown) {
      setMessage(error instanceof Error ? error.message : "테스트 결제를 취소하지 못했습니다.");
    } finally {
      setCanceling(false);
    }
  }

  return (
    <div className="dashboard-shell">
      <AppHeader />
      <main className="dashboard-content toss-result-shell">
        <section className={`toss-result-card ${state}`} aria-live="polite">
          <span className="toss-result-icon" aria-hidden="true">{state === "success" ? "✓" : state === "failure" ? "!" : "···"}</span>
          <p className="eyebrow">TOSS TEST PAYMENT</p>
          <h1>{state === "success" ? "자동 수집을 시작했습니다" : state === "failure" ? "승인을 확인해 주세요" : "결제를 승인하고 있습니다"}</h1>
          <p className="muted">{message}</p>
          {state === "success" && <div className="toss-result-actions">
            <Link className="primary-button" href="/transactions">소비 내역 확인</Link>
            <button className="cancel-demo-button" type="button" onClick={cancelPayment} disabled={canceling || canceled}>
              {canceled ? "취소 접수 완료" : canceling ? "취소 처리 중..." : "전체 취소도 시연하기"}
            </button>
            <Link className="secondary-link" href="/dashboard">대시보드 보기</Link>
          </div>}
          {state === "failure" && <Link className="primary-button" href="/toss-test">다시 테스트하기</Link>}
        </section>
      </main>
    </div>
  );
}
