import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { TransactionHistoryView } from "@/features/transactions/TransactionHistoryView";
import { SESSION_COOKIES } from "@/lib/server/backend";
import { currentServiceMonth } from "@/lib/serviceTime";

export default async function TransactionsPage() {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) {
    redirect("/login");
  }

  return <TransactionHistoryView initialMonth={currentServiceMonth()} />;
}
