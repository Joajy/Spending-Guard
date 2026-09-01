import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { AppHeader } from "./AppHeader";

const replace = vi.fn();
const refresh = vi.fn();
let pathname = "/dashboard";

vi.mock("next/navigation", () => ({
  usePathname: () => pathname,
  useRouter: () => ({ replace, refresh }),
}));

describe("AppHeader", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
    pathname = "/dashboard";
  });

  it("shows every service destination and marks the current page", () => {
    pathname = "/transactions/new";
    render(<AppHeader />);

    expect(screen.getByRole("navigation", { name: "주요 메뉴" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "소비 등록" })).toHaveAttribute("aria-current", "page");
    expect(screen.getByRole("link", { name: "소비 내역" })).not.toHaveAttribute("aria-current");
    expect(screen.getAllByRole("link")).toHaveLength(6);
  });

  it("opens the mobile menu and closes it after navigation", async () => {
    const user = userEvent.setup();
    render(<AppHeader />);

    const toggle = screen.getByRole("button", { name: "메뉴 열기" });
    await user.click(toggle);
    expect(toggle).toHaveAttribute("aria-expanded", "true");

    const budgetLink = screen.getByRole("link", { name: "예산" });
    budgetLink.addEventListener("click", (event) => event.preventDefault());
    await user.click(budgetLink);
    expect(screen.getByRole("button", { name: "메뉴 열기" })).toHaveAttribute("aria-expanded", "false");
  });

  it("clears the browser session and moves to login", async () => {
    const user = userEvent.setup();
    vi.spyOn(global, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
    render(<AppHeader />);

    await user.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(fetch).toHaveBeenCalledWith("/api/session", { method: "DELETE" });
    expect(refresh).toHaveBeenCalled();
  });

  it("moves to login even when the logout request fails", async () => {
    vi.spyOn(global, "fetch").mockRejectedValue(new Error("offline"));
    render(<AppHeader />);

    fireEvent.click(screen.getByRole("button", { name: "로그아웃" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/login"));
    expect(refresh).toHaveBeenCalled();
  });
});
