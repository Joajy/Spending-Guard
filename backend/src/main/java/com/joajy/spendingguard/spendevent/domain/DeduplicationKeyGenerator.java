package com.joajy.spendingguard.spendevent.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

@Component
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
