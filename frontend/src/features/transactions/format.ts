import type {
  RiskLevel,
  SpendCategory,
  SpendEventSource,
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
  COMPLETED: "분석 완료",
  NEEDS_REVIEW: "확인 필요",
};

export const RISK_LABELS: Record<RiskLevel, string> = {
  LOW: "안정",
  MEDIUM: "주의",
  HIGH: "높음",
};

export const SOURCE_LABELS: Record<SpendEventSource, string> = {
  MANUAL_TEXT: "직접 입력",
  SIMULATOR: "테스트 연동",
  TOSS_WEBHOOK: "Toss 자동 연동",
};

const RISK_REASON_LABELS: Record<string, string> = {
  NON_PAYMENT: "결제가 아닌 취소·환불 거래입니다.",
  LARGE_PAYMENT: "일반적인 소비보다 금액이 큰 결제입니다.",
  LATE_NIGHT_TRANSPORT: "심야 시간대의 교통비 결제입니다.",
  ELEVATED_AMOUNT: "주의해서 확인할 금액대의 결제입니다.",
  HIGH_DELIVERY_AMOUNT: "배달 카테고리의 비교적 큰 결제입니다.",
  NORMAL_PATTERN: "현재 규칙에서 특이한 위험 신호가 발견되지 않았습니다.",
};

const REVIEW_REASON_LABELS: Record<string, string> = {
  EMPTY_MESSAGE: "알림 내용이 비어 있습니다.",
  AMOUNT_NOT_FOUND: "결제 금액을 찾지 못했습니다.",
  TRANSACTION_TYPE_NOT_FOUND: "결제·취소·환불 여부를 판단하지 못했습니다.",
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

export function formatFullDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "시각 확인 중";
  return new Intl.DateTimeFormat("ko-KR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
    timeZone: "Asia/Seoul",
  }).format(date);
}

export function riskReasonLabel(reason: string | null): string {
  if (!reason) return "아직 위험 판정 결과가 없습니다.";
  return RISK_REASON_LABELS[reason] ?? "분석 규칙에 따라 확인이 필요한 소비로 분류되었습니다.";
}

export function reviewReasonLabel(reason: string | null): string {
  if (!reason) return "사용자 확인이 필요한 항목이 있습니다.";
  return reason.split(",")
    .map((code) => REVIEW_REASON_LABELS[code] ?? code)
    .join(" ");
}
