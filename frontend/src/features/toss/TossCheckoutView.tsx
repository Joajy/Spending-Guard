"use client";

import Script from "next/script";
import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { AppHeader } from "@/features/navigation/AppHeader";

type CreatedOrder = {
  orderId: string;
  clientKey: string;
  amount: number;
  orderName: string;
};

type PaymentRequest = {
  method: "CARD";
  amount: { currency: "KRW"; value: number };
  orderId: string;
  orderName: string;
  successUrl: string;
  failUrl: string;
};

type TossPaymentsFactory = (clientKey: string) => {
  payment: (options: { customerKey: "ANONYMOUS" }) => {
    requestPayment: (request: PaymentRequest) => Promise<void>;
  };
};

export function TossCheckoutView() {
  const router = useRouter();
  const [sdkReady, setSdkReady] = useState(false);
  const [pending, setPending] = useState(false);
  const [message, setMessage] = useState("");

  async function requestPayment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!sdkReady || pending) return;
    const form = new FormData(event.currentTarget);
    const amount = Number(form.get("amount"));
    const orderName = String(form.get("orderName") ?? "").trim();
    setPending(true);
    setMessage("");
    try {
      const response = await fetch("/api/toss-test/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ amount, orderName }),
      });
      if (response.status === 401) {
        router.replace("/login");
        router.refresh();
        return;
      }
      if (!response.ok) {
        const body = await response.json() as { message?: string };
        setMessage(body.message ?? "테스트 주문을 만들지 못했습니다.");
        return;
      }
      const order = await response.json() as CreatedOrder;
      const factory = (window as unknown as { TossPayments?: TossPaymentsFactory }).TossPayments;
      if (!factory) throw new Error("Toss SDK is not ready");
      const payment = factory(order.clientKey).payment({ customerKey: "ANONYMOUS" });
      await payment.requestPayment({
        method: "CARD",
        amount: { currency: "KRW", value: order.amount },
        orderId: order.orderId,
        orderName: order.orderName,
        successUrl: `${window.location.origin}/toss-test/success`,
        failUrl: `${window.location.origin}/toss-test/fail`,
      });
    } catch {
      setMessage("Toss 결제창을 열지 못했습니다. 설정과 네트워크를 확인해 주세요.");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="dashboard-shell">
      <Script
        src="https://js.tosspayments.com/v2/standard"
        strategy="afterInteractive"
        onLoad={() => setSdkReady(true)}
        onError={() => setMessage("Toss 결제 SDK를 불러오지 못했습니다.")}
      />
      <AppHeader />
      <main className="dashboard-content toss-test-content">
        <div className="dashboard-title-row">
          <div>
            <p className="eyebrow">TOSS PAYMENTS SANDBOX</p>
            <h1>자동 수집 시연</h1>
            <p className="muted">가상 카드 결제를 만들고 Spending Guard 반영까지 확인합니다.</p>
          </div>
          <span className={`sdk-status ${sdkReady ? "ready" : ""}`}>
            {sdkReady ? "Toss SDK 준비됨" : "SDK 불러오는 중"}
          </span>
        </div>
        <div className="toss-demo-grid">
          <form className="toss-order-card" onSubmit={requestPayment}>
            <span className="demo-step">01 · TEST ORDER</span>
            <h2>테스트 주문 만들기</h2>
            <label htmlFor="orderName">표시할 주문명</label>
            <input id="orderName" name="orderName" defaultValue="Spending Guard 자동수집 테스트" maxLength={100} />
            <label htmlFor="amount">테스트 결제 금액</label>
            <div className="amount-input"><input id="amount" name="amount" type="number" min={100} max={1_000_000} defaultValue={12_800} required /><span>원</span></div>
            {message && <p className="form-error" role="alert">{message}</p>}
            <button className="primary-button toss-pay-button" type="submit" disabled={!sdkReady || pending}>
              {pending ? "결제창 여는 중..." : "Toss 테스트 결제하기"}
            </button>
            <p className="field-help">테스트 환경에서는 실제 카드 대금이 청구되지 않습니다.</p>
          </form>
          <section className="toss-flow-card" aria-label="자동 수집 처리 단계">
            <span className="demo-step">02 · AUTOMATION FLOW</span>
            <h2>결제 후 자동으로 진행됩니다</h2>
            <ol className="toss-flow-list">
              <li><b>1</b><div><strong>Toss 결제 승인</strong><span>서버에 저장한 주문 금액과 대조</span></div></li>
              <li><b>2</b><div><strong>웹훅 재검증</strong><span>paymentKey로 Toss 원본 결제 조회</span></div></li>
              <li><b>3</b><div><strong>Kafka 비동기 분석</strong><span>중복 없이 금액·카테고리 분석</span></div></li>
              <li><b>4</b><div><strong>예산 자동 반영</strong><span>대시보드와 소비 내역에서 확인</span></div></li>
            </ol>
          </section>
        </div>
      </main>
    </div>
  );
}
