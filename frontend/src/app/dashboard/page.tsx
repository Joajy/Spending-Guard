import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { DashboardView } from "@/features/dashboard/DashboardView";
import { SESSION_COOKIES } from "@/lib/server/backend";

export default async function DashboardPage() {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) {
    redirect("/login");
  }

  const now = new Date();
  const initialMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
  return <DashboardView initialMonth={initialMonth} />;
}
