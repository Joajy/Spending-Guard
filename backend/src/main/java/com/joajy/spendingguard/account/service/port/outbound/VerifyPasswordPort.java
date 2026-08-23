package com.joajy.spendingguard.account.service.port.outbound;

/** 평문 비밀번호를 저장된 단방향 해시와 안전하게 비교한다. */
public interface VerifyPasswordPort {
    boolean matches(String rawPassword, String passwordHash);
}
