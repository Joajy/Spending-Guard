package com.joajy.spendingguard.account.domain.policy;

import java.util.Locale;

/** 이메일 앞뒤 공백과 대소문자 차이를 제거해 계정 중복 판정 기준을 고정한다. */
public class EmailNormalizer {

    public String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
