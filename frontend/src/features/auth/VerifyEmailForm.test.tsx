import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { VerifyEmailForm } from "./VerifyEmailForm";

describe("VerifyEmailForm", () => {
  beforeEach(() => vi.restoreAllMocks());

  it("shows the validity guidance and a resend cooldown after initial delivery", () => {
    render(<VerifyEmailForm initiallySent />);
    expect(screen.getByRole("status")).toHaveTextContent("5분");
    expect(screen.getByRole("button", { name: /다시 받기 \(60초\)/ })).toBeDisabled();
  });

  it("keeps only six digits and completes verification", async () => {
    const request = vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 204 }));
    const user = userEvent.setup();
    render(<VerifyEmailForm initiallySent />);
    await user.type(screen.getByLabelText("인증번호"), "12ab34567");
    expect(screen.getByLabelText("인증번호")).toHaveValue("123456");
    await user.click(screen.getByRole("button", { name: "인증 완료" }));

    expect(await screen.findByRole("status")).toHaveTextContent("인증을 마쳤습니다");
    expect(screen.getByRole("link", { name: "로그인하러 가기" })).toHaveAttribute("href", "/login");
    expect(request).toHaveBeenCalledWith("/api/email-verification", expect.objectContaining({
      method: "PUT",
      body: JSON.stringify({ code: "123456" }),
    }));
  });

  it("rejects an incomplete code before making a request", async () => {
    const request = vi.spyOn(globalThis, "fetch");
    render(<VerifyEmailForm initiallySent={false} />);
    fireEvent.change(screen.getByLabelText("인증번호"), { target: { value: "123" } });
    fireEvent.submit(screen.getByRole("button", { name: "인증 완료" }).closest("form")!);
    expect(await screen.findByRole("alert")).toHaveTextContent("6자리");
    expect(request).not.toHaveBeenCalled();
  });

  it("shows an expired or incorrect code response", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "인증번호가 올바르지 않거나 만료되었습니다." }), { status: 422 }),
    );
    render(<VerifyEmailForm initiallySent={false} />);
    fireEvent.change(screen.getByLabelText("인증번호"), { target: { value: "123456" } });
    fireEvent.submit(screen.getByRole("button", { name: "인증 완료" }).closest("form")!);
    expect(await screen.findByRole("alert")).toHaveTextContent("만료");
  });

  it("requests a new code and starts the cooldown", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(null, { status: 202 }));
    const user = userEvent.setup();
    render(<VerifyEmailForm initiallySent={false} />);
    await user.click(screen.getByRole("button", { name: "인증번호 다시 받기" }));
    await waitFor(() => expect(screen.getByRole("button", { name: /다시 받기 \(60초\)/ })).toBeDisabled());
    expect(screen.getByRole("status")).toHaveTextContent("새 인증번호");
  });

  it("shows a resend failure without starting the cooldown", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ message: "인증번호는 60초 후 다시 요청할 수 있습니다." }), { status: 429 }),
    );
    const user = userEvent.setup();
    render(<VerifyEmailForm initiallySent={false} />);
    await user.click(screen.getByRole("button", { name: "인증번호 다시 받기" }));
    expect(await screen.findByRole("alert")).toHaveTextContent("60초 후");
    expect(screen.getByRole("button", { name: "인증번호 다시 받기" })).toBeEnabled();
  });
});
