package com.joajy.spendingguard.account.api.dto.response;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.application.result.UserRegistration;

/** 비밀번호 정보를 제외한 회원 등록 성공 응답이다. */
public record UserRegisteredResponse(UUID userId, String email, Instant createdAt) {

    public static UserRegisteredResponse from(UserRegistration registration) {
        return new UserRegisteredResponse(
                registration.userId(),
                registration.email(),
                registration.createdAt()
        );
    }
}
