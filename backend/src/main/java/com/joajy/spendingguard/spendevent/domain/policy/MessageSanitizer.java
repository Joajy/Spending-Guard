package com.joajy.spendingguard.spendevent.domain.policy;

import java.util.regex.Pattern;

/**
 * 소비 알림을 저장하기 전에 이메일과 계좌·카드 번호로 보이는 긴 숫자열을 마스킹한다.
 * 공백도 함께 정규화해 중복 비교와 후속 텍스트 분석이 입력 형식의 차이에 덜 영향을 받도록 한다.
 * 규칙 기반 1차 보호 수단이며, 실제 금융 연동 시에는 채널별 민감 정보 정책을 추가 적용해야 한다.
 */
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
