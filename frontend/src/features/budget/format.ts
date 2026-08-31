const MAX_BUDGET = 1_000_000_000_000;

export function formatBudgetInput(value: string): string {
  const digits = value.replace(/\D/g, "").replace(/^0+(?=\d)/, "").slice(0, 13);
  return digits ? Number(digits).toLocaleString("ko-KR") : "";
}

export function parseBudgetAmount(value: string): number | null {
  const normalized = value.replaceAll(",", "");
  if (!/^\d+$/.test(normalized)) return null;
  const amount = Number(normalized);
  return Number.isSafeInteger(amount) && amount >= 1 && amount <= MAX_BUDGET ? amount : null;
}
