package com.joajy.spendingguard.spendevent.domain.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 같은 소비 알림을 반복 접수하지 않도록 결정적인 SHA-256 중복 키를 생성한다.
 *
 * <p><strong>키 우선순위:</strong> 외부 ID가 있으면 {@code source + externalEventId}를
 * 사용한다. 없으면 {@code source + normalizedMessage + occurredAt}으로 대체 지문을 만든다.
 * 채널이 다른 동일 문자열은 별개 이벤트로 취급한다.
 *
 * <p><strong>보안 경계:</strong> 반환값은 64자리 16진수 해시이므로 데이터베이스의 중복
 * 인덱스에 원문을 남기지 않는다. 다만 SHA-256은 암호화가 아니므로 입력 후보가 제한된
 * 환경에서 원문 비밀성을 보장하는 수단으로 사용해서는 안 된다.
 */
public class DeduplicationKeyGenerator {

    /**
     * 입력 의미가 같으면 항상 같은 중복 키를 생성한다.
     *
     * @param source 이벤트 수집 채널
     * @param externalEventId 채널이 제공한 식별자, 없으면 {@code null}
     * @param message 정규화 전 소비 알림
     * @param occurredAt 외부 채널 기준 발생 시각
     * @return SHA-256 해시를 소문자 16진수로 표현한 64자리 키
     */
    public String generate(
            SpendEventSource source,
            String externalEventId,
            String message,
            Instant occurredAt
    ) {
        String sourceValue;
        if (externalEventId != null) {
            sourceValue = source.name() + "|external|" + externalEventId;
        } else {
            sourceValue = source.name()
                    + "|message|" + normalizeMessage(message)
                    + "|occurredAt|" + (occurredAt == null ? "" : occurredAt);
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(sourceValue.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String normalizeMessage(String message) {
        return message.trim().replaceAll("\\s+", " ");
    }
}
