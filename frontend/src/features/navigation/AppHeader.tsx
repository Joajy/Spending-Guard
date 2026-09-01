"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";

const NAVIGATION_ITEMS = [
  { href: "/dashboard", label: "대시보드" },
  { href: "/transactions/new", label: "소비 등록" },
  { href: "/transactions", label: "소비 내역" },
  { href: "/budget", label: "예산" },
  { href: "/alerts", label: "위험 알림" },
] as const;

export function AppHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  async function logout() {
    if (loggingOut) return;
    setLoggingOut(true);
    await fetch("/api/session", { method: "DELETE" }).catch(() => undefined);
    router.replace("/login");
    router.refresh();
  }

  return (
    <header className="dashboard-header">
      <Link className="brand-lockup" href="/dashboard" onClick={() => setMenuOpen(false)}>
        <span className="brand-mark">SG</span><strong>Spending Guard</strong>
      </Link>
      <button
        className="navigation-toggle"
        type="button"
        aria-label={menuOpen ? "메뉴 닫기" : "메뉴 열기"}
        aria-expanded={menuOpen}
        aria-controls="app-navigation"
        onClick={() => setMenuOpen((current) => !current)}
      >
        <span /><span /><span />
      </button>
      <nav id="app-navigation" className={`header-actions${menuOpen ? " open" : ""}`} aria-label="주요 메뉴">
        {NAVIGATION_ITEMS.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={isCurrentPath(pathname, item.href) ? "active" : undefined}
            aria-current={isCurrentPath(pathname, item.href) ? "page" : undefined}
            onClick={() => setMenuOpen(false)}
          >{item.label}</Link>
        ))}
        <button className="text-button" type="button" onClick={logout} disabled={loggingOut}>
          {loggingOut ? "로그아웃 중..." : "로그아웃"}
        </button>
      </nav>
    </header>
  );
}

function isCurrentPath(pathname: string, href: string): boolean {
  if (href === "/transactions") return pathname === href;
  return pathname === href || pathname.startsWith(`${href}/`);
}
