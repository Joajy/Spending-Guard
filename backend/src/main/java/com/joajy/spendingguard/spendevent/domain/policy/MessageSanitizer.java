package com.joajy.spendingguard.spendevent.domain.policy;

import java.util.regex.Pattern;

public class MessageSanitizer {

    private static final Pattern LONG_NUMBER = Pattern.compile(
            "(?<!\\d)(?:\\d{4}(?:[- ]\\d{4}){3}|\\d{2,6}(?:-\\d{2,6}){2,5}|\\d{10,19})(?!\\d)"
    );
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    public String sanitize(String message) {
        String normalized = WHITESPACE.matcher(message.trim()).replaceAll(" ");
        String withoutEmail = EMAIL.matcher(normalized).replaceAll("[EMAIL]");
        return LONG_NUMBER.matcher(withoutEmail).replaceAll("[REDACTED]");
    }
}
