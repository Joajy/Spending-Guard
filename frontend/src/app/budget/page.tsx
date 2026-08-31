import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { MonthlyBudgetView } from "@/features/budget/MonthlyBudgetView";
import { SESSION_COOKIES } from "@/lib/server/backend";
import { currentServiceMonth } from "@/lib/serviceTime";

const MONTH_PATTERN = /^\d{4}-(0[1-9]|1[0-2])$/;

type Props = { searchParams: Promise<{ month?: string }> };

export default async function BudgetPage({ searchParams }: Props) {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) {
    redirect("/login");
  }
  const requestedMonth = (await searchParams).month ?? "";
  const initialMonth = MONTH_PATTERN.test(requestedMonth) ? requestedMonth : currentServiceMonth();

  return <MonthlyBudgetView initialMonth={initialMonth} />;
}
