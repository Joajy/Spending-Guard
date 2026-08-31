export type BudgetSummary = {
  limitAmount: number;
  spentAmount: number;
  remainingAmount: number;
};

export type CategorySpending = {
  category: string;
  amount: number;
  transactionCount: number;
};

export type RiskCount = {
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  count: number;
};

export type MonthlyDashboard = {
  month: string;
  budget: BudgetSummary | null;
  totalSpending: number;
  transactionCount: number;
  categories: CategorySpending[];
  risks: RiskCount[];
};
