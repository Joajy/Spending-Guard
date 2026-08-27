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
export type SpendEventSource = "MANUAL_TEXT" | "SIMULATOR";
export type FastParseStatus = "PARSED" | "NEEDS_REVIEW";

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

export type FastParseResult = {
  amount: number | null;
  currency: string;
  transactionType: TransactionType | null;
  status: FastParseStatus;
  reviewReason: string | null;
  category: SpendCategory | null;
  fixedCost: boolean | null;
  riskLevel: RiskLevel | null;
  riskReason: string | null;
  parserVersion: string;
  parsedAt: string;
  categoryVersion: number;
};

export type SpendEventDetail = {
  eventId: string;
  source: SpendEventSource;
  status: SpendEventStatus;
  occurredAt: string;
  receivedAt: string;
  fastParse: FastParseResult | null;
};

export type CategoryCorrectionResult = {
  eventId: string;
  originalCategory: SpendCategory;
  category: SpendCategory;
  fixedCost: boolean;
  riskLevel: RiskLevel;
  riskReason: string;
  version: number;
  correctedAt: string;
};
