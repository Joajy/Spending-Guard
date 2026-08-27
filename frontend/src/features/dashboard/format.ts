const wonFormatter = new Intl.NumberFormat("ko-KR");

export function formatWon(amount: number): string {
  return `${wonFormatter.format(amount)}원`;
}

export function budgetProgress(spent: number, limit: number): number {
  if (limit <= 0) return 0;
  return Math.min(100, Math.round((spent / limit) * 100));
}

export function monthLabel(month: string): string {
  const [year, value] = month.split("-");
  return `${year}년 ${Number(value)}월`;
}

export function moveMonth(month: string, offset: number): string {
  const [year, value] = month.split("-").map(Number);
  const date = new Date(Date.UTC(year, value - 1 + offset, 1));
  return `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, "0")}`;
}
