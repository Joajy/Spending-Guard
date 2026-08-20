package com.joajy.spendingguard.account.api.dto.request;

import com.joajy.spendingguard.account.application.command.RegisterUserCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 회원 등록 요청의 이메일 형식과 BCrypt 입력 길이를 검증한다. */
public record RegisterUserRequest(
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank @Size(min = 8, max = 72) String password
) {

    public RegisterUserRequest {
        if (email != null) {
            email = email.trim();
        }
    }

    public RegisterUserCommand toCommand() {
        return new RegisterUserCommand(email, password);
    }
}
