import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { TossCheckoutView } from "@/features/toss/TossCheckoutView";
import { SESSION_COOKIES } from "@/lib/server/backend";

export default async function TossTestPage() {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) redirect("/login");
  return <TossCheckoutView />;
}
