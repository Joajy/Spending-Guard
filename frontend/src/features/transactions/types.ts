export type SpendEventStatus = "RECEIVED" | "ANALYZING" | "NEEDS_REVIEW";
export type SpendCategory =
  | "SUBSCRIPTION"
  | "TRANSPORT"
  | "DELIVERY"
  | "SHOPPING"
  | "TRANSFER"
  | "OTHER";
export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";
export type TransactionType = "PAYMENT" | "CANCEL" | "REFUND";

export type SpendEventHistoryItem = {
  eventId: string;
  displayText: string;
  status: SpendEventStatus;
  transactionAt: string | null;
  amount: number | null;
  currency: string | null;
  transactionType: TransactionType | null;
  category: SpendCategory | null;
  fixedCost: boolean | null;
  riskLevel: RiskLevel | null;
  categoryVersion: number;
};
export type SpendEventHistoryPage = {
  items: SpendEventHistoryItem[];
  nextCursor: string | null;
  hasNext: boolean;
};
