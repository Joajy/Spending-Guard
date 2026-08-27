import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { RiskAlertFeed } from "@/features/risk-alerts/RiskAlertFeed";
import { SESSION_COOKIES } from "@/lib/server/backend";
import { currentServiceMonth } from "@/lib/serviceTime";

export default async function AlertsPage() {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) {
    redirect("/login");
  }

  return <RiskAlertFeed initialMonth={currentServiceMonth()} />;
}
