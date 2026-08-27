import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Spending Guard",
  description: "소비 흐름과 위험 신호를 한눈에 확인합니다.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="ko">
      <body>{children}</body>
    </html>
  );
}
