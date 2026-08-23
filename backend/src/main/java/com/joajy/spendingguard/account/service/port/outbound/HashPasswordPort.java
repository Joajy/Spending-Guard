package com.joajy.spendingguard.account.service.port.outbound;

/** 애플리케이션 계층이 특정 암호화 라이브러리에 의존하지 않도록 분리한 비밀번호 해시 포트다. */
public interface HashPasswordPort {

    String hash(String rawPassword);
}
