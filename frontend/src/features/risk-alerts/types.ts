import type { RiskLevel, SpendCategory } from "@/features/transactions/types";

export type RiskAlertItem = {
  eventId: string;
  displayText: string;
  transactionAt: string;
  amount: number;
  category: SpendCategory;
  riskLevel: Exclude<RiskLevel, "LOW">;
  reasonCode: string;
  categoryVersion: number;
};

export type RiskAlertPage = {
  items: RiskAlertItem[];
  nextCursor: string | null;
  hasNext: boolean;
};

export type MinimumRiskLevel = "MEDIUM" | "HIGH";
