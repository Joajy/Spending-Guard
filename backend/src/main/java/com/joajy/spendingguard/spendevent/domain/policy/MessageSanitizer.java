package com.joajy.spendingguard.spendevent.domain.policy;

import java.util.regex.Pattern;

/**
 * 소비 알림을 저장하기 전에 이메일과 계좌·카드 번호로 보이는 긴 숫자열을 마스킹한다.
 *
 * <p>앞뒤 공백을 제거하고 연속 공백을 하나로 합친 뒤, 이메일은 {@code [EMAIL]},
 * 카드·계좌 형식과 10~19자리 숫자열은 {@code [REDACTED]}로 치환한다.
 *
 * <p>이 정책은 알려진 문자열 형태를 줄이는 저장 전 1차 보호선이다. 이름, 주소,
 * 새로운 금융사 포맷까지 완전하게 탐지하지 않으므로 실제 연동 채널에는 데이터 분류와
 * 채널별 마스킹 규칙을 추가해야 한다.
 */
public class MessageSanitizer {

    private static final Pattern LONG_NUMBER = Pattern.compile(
            "(?<!\\d)(?:\\d{4}(?:[- ]\\d{4}){3}|\\d{2,6}(?:-\\d{2,6}){2,5}|\\d{10,19})(?!\\d)"
    );
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * 소비 알림을 정규화하고 알려진 민감 문자열을 대체한다.
     *
     * @param message 저장 전 원본 소비 알림
     * @return 공백이 정규화되고 민감 패턴이 대체된 문자열
     */
    public String sanitize(String message) {
        String normalized = WHITESPACE.matcher(message.trim()).replaceAll(" ");
        String withoutEmail = EMAIL.matcher(normalized).replaceAll("[EMAIL]");
        return LONG_NUMBER.matcher(withoutEmail).replaceAll("[REDACTED]");
    }
}
