package com.joajy.spendingguard.account.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * 서비스 사용자의 로그인 식별자와 비밀번호 해시를 보관하는 계정 모델이다.
 *
 * <p>평문 비밀번호는 이 모델의 경계를 통과하지 않는다. 애플리케이션 서비스에서 해시가
 * 만들어진 뒤에만 생성되며, API 응답에는 {@code passwordHash}를 노출하지 않는다.
 */
public record UserAccount(
        UUID id,
        String email,
        String passwordHash,
        Instant createdAt
) {
}
