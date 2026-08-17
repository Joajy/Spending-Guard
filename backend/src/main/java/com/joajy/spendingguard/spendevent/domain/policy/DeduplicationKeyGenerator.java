package com.joajy.spendingguard.spendevent.domain.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventSource;

/**
 * 같은 소비 알림을 반복 접수하지 않도록 결정적인 SHA-256 중복 키를 생성한다.
 * 금융사가 제공한 외부 ID가 있으면 유입 경로와 ID를 우선 사용하고, 없으면 공백을 정규화한 메시지와 발생 시각으로 지문을 만든다.
 * 키 자체에는 원문을 남기지 않아 중복 검사 과정에서 불필요한 민감 정보 노출을 줄인다.
 */
public class DeduplicationKeyGenerator {

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
