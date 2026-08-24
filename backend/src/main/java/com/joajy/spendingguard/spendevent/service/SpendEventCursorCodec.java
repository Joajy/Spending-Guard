package com.joajy.spendingguard.spendevent.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.service.exception.InvalidSpendEventQueryException;

/** 거래 시각과 이벤트 ID를 외부에 노출되지 않는 URL-safe 페이지 커서로 변환한다. */
final class SpendEventCursorCodec {

    private SpendEventCursorCodec() {
    }

    static String encode(Instant transactionAt, UUID eventId) {
        String value = transactionAt + "|" + eventId;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    static Cursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            String value = new String(
                    Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8
            );
            int separator = value.indexOf('|');
            if (separator < 1) {
                throw new IllegalArgumentException();
            }
            return new Cursor(
                    Instant.parse(value.substring(0, separator)),
                    UUID.fromString(value.substring(separator + 1))
            );
        } catch (IllegalArgumentException exception) {
            throw new InvalidSpendEventQueryException("cursor 형식을 확인해 주세요.");
        }
    }

    record Cursor(Instant transactionAt, UUID eventId) {
    }
}
