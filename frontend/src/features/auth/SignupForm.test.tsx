import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { SignupForm } from "./SignupForm";

const replace = vi.fn();
const refresh = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ replace, refresh }),
}));

async function fillValidForm() {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText("이메일"), "member@example.com");
  await user.type(screen.getByLabelText("비밀번호", { selector: "#password" }), "safe-password");
  await user.type(screen.getByLabelText("비밀번호 확인"), "safe-password");
  return user;
}

describe("SignupForm", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
    replace.mockReset();
    refresh.mockReset();
  });

  it("registers, requests a verification code, and moves to verification", async () => {
    const request = vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(JSON.stringify({ email: "member@example.com" }), { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 202 }));
    render(<SignupForm />);
    const user = await fillValidForm();

    await user.click(screen.getByRole("button", { name: "계정 만들기" }));

    await waitFor(() => expect(replace).toHaveBeenCalledWith("/verify-email?sent=1"));
    expect(refresh).toHaveBeenCalled();
    expect(request).toHaveBeenNthCalledWith(1, "/api/registration", expect.objectContaining({
      body: JSON.stringify({ email: "member@example.com", password: "safe-password" }),
    }));
    expect(request).toHaveBeenNthCalledWith(2, "/api/email-verification", { method: "POST" });
  });

  it("keeps a successfully created account recoverable when mail delivery fails", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 503 }));
    render(<SignupForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "계정 만들기" }));
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/verify-email?sent=0"));
  });

  it("moves to a retryable verification step when the delivery request loses its connection", async () => {
    vi.spyOn(globalThis, "fetch")
      .mockResolvedValueOnce(new Response(null, { status: 201 }))
      .mockRejectedValueOnce(new Error("mail gateway offline"));
    render(<SignupForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "계정 만들기" }));
    await waitFor(() => expect(replace).toHaveBeenCalledWith("/verify-email?sent=0"));
  });

  it("shows a duplicate account message without requesting a code", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "이미 가입된 이메일입니다." }), { status: 409 }),
    );
    render(<SignupForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "계정 만들기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("이미 가입된 이메일");
    expect(request).toHaveBeenCalledTimes(1);
  });

  it("catches mismatched passwords before making a request", async () => {
    const request = vi.spyOn(globalThis, "fetch");
    render(<SignupForm />);
    fireEvent.change(screen.getByLabelText("이메일"), { target: { value: "member@example.com" } });
    fireEvent.change(screen.getByLabelText("비밀번호", { selector: "#password" }), { target: { value: "safe-password" } });
    fireEvent.change(screen.getByLabelText("비밀번호 확인"), { target: { value: "different-password" } });
    fireEvent.submit(screen.getByRole("button", { name: "계정 만들기" }).closest("form")!);
    expect(await screen.findByRole("alert")).toHaveTextContent("일치하지 않습니다");
    expect(request).not.toHaveBeenCalled();
  });

  it("shows a network-safe error", async () => {
    vi.spyOn(globalThis, "fetch").mockRejectedValue(new Error("offline"));
    render(<SignupForm />);
    const user = await fillValidForm();
    await user.click(screen.getByRole("button", { name: "계정 만들기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("네트워크 연결");
  });
});
