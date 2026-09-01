import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { SpendEventSubmissionView } from "@/features/transactions/SpendEventSubmissionView";
import { SESSION_COOKIES } from "@/lib/server/backend";

export default async function NewTransactionPage() {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) redirect("/login");
  return <SpendEventSubmissionView />;
}
