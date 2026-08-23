package com.joajy.spendingguard.account.service.result;

import java.time.Instant;
import java.util.UUID;

/** 회원 등록 완료 후 외부에 공개할 수 있는 계정 정보다. */
public record UserRegistration(UUID userId, String email, Instant createdAt) {
}
