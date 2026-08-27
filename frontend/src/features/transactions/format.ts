import type {
  RiskLevel,
  SpendCategory,
  SpendEventStatus,
  TransactionType,
} from "./types";

export const CATEGORY_LABELS: Record<SpendCategory, string> = {
  SUBSCRIPTION: "구독",
  TRANSPORT: "교통",
  DELIVERY: "배달",
  SHOPPING: "쇼핑",
  TRANSFER: "이체",
  OTHER: "기타",
};

export const STATUS_LABELS: Record<SpendEventStatus, string> = {
  RECEIVED: "접수됨",
  ANALYZING: "분석 중",
  NEEDS_REVIEW: "확인 필요",
};

export const RISK_LABELS: Record<RiskLevel, string> = {
  LOW: "안정",
  MEDIUM: "주의",
  HIGH: "높음",
};

export function formatTransactionAmount(
  amount: number | null,
  transactionType: TransactionType | null,
): string {
  if (amount === null || transactionType === null) return "금액 확인 중";
  const prefix = transactionType === "PAYMENT" ? "-" : "+";
  return `${prefix}${amount.toLocaleString("ko-KR")}원`;
}
export function formatTransactionTime(value: string | null): string {
  if (!value) return "거래 시각 확인 중";
  return new Intl.DateTimeFormat("ko-KR", {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: "Asia/Seoul",
  }).format(new Date(value));
}
