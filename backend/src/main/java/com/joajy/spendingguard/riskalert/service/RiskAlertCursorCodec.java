package com.joajy.spendingguard.riskalert.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

import com.joajy.spendingguard.riskalert.service.exception.InvalidRiskAlertQueryException;

final class RiskAlertCursorCodec {

    private RiskAlertCursorCodec() {
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
            String[] parts = value.split("\\|", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException();
            }
            return new Cursor(Instant.parse(parts[0]), UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException exception) {
            throw new InvalidRiskAlertQueryException("cursor 값을 확인해 주세요.");
        }
    }

    record Cursor(Instant transactionAt, UUID eventId) {
    }
}
