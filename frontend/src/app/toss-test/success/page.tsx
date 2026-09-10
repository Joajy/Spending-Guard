import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { TossConfirmationView } from "@/features/toss/TossConfirmationView";
import { SESSION_COOKIES } from "@/lib/server/backend";

type SearchParams = Promise<Record<string, string | string[] | undefined>>;

export default async function TossSuccessPage({ searchParams }: { searchParams: SearchParams }) {
  const cookieStore = await cookies();
  if (!cookieStore.has(SESSION_COOKIES.user)) redirect("/login");
  const query = await searchParams;
  return <TossConfirmationView
    paymentKey={single(query.paymentKey)}
    orderId={single(query.orderId)}
    amount={single(query.amount)}
  />;
}

function single(value: string | string[] | undefined): string {
  return typeof value === "string" ? value : "";
}
