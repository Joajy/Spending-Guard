import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { LoginForm } from "./LoginForm";

const replace = vi.fn();
const refresh = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, refresh }),
}));

describe("LoginForm", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("sends credentials and moves to the dashboard", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ userId: "user-1" }), { status: 200 }),
    );
    const user = userEvent.setup();
    render(<LoginForm />);

    await user.type(screen.getByLabelText("이메일"), "member@example.com");
    await user.type(screen.getByLabelText("비밀번호"), "safe-password");
    await user.click(screen.getByRole("button", { name: "로그인" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/dashboard"));
    expect(refresh).toHaveBeenCalled();
    expect(request).toHaveBeenCalledWith("/api/session", expect.objectContaining({
      method: "POST",
      body: JSON.stringify({ email: "member@example.com", password: "safe-password" }),
    }));
  });

  it("shows the server message when authentication fails", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "이메일 인증을 완료해 주세요." }), { status: 403 }),
    );
    render(<LoginForm />);
    fireEvent.change(screen.getByLabelText("이메일"), { target: { value: "member@example.com" } });
    fireEvent.change(screen.getByLabelText("비밀번호"), { target: { value: "password" } });
    fireEvent.submit(screen.getByRole("button", { name: "로그인" }).closest("form")!);

    expect(await screen.findByRole("alert")).toHaveTextContent("이메일 인증을 완료해 주세요.");
    expect(replace).not.toHaveBeenCalled();
  });

  it("shows a network-safe error", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));
    render(<LoginForm />);
    fireEvent.change(screen.getByLabelText("이메일"), { target: { value: "member@example.com" } });
    fireEvent.change(screen.getByLabelText("비밀번호"), { target: { value: "password" } });
    fireEvent.submit(screen.getByRole("button", { name: "로그인" }).closest("form")!);

    expect(await screen.findByRole("alert")).toHaveTextContent("네트워크 연결");
  });
});
