package com.joajy.spendingguard.account.infrastructure.security;

import com.joajy.spendingguard.account.application.port.outbound.HashPasswordPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt work factor 12로 비밀번호를 단방향 해시하는 보안 어댑터다.
 *
 * <p>같은 비밀번호도 매번 다른 salt를 사용하므로 해시 문자열을 직접 비교하지 않는다.
 * 로그인 기능은 후속 작업에서 BCrypt의 {@code matches} 연산을 별도 포트로 노출한다.
 */
@Component
class BCryptPasswordHasher implements HashPasswordPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }
}
