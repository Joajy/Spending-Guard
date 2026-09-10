package com.joajy.spendingguard.integration.toss.config;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/** Toss Payments 테스트 상점과 Spending Guard 사용자 사이의 연결 설정이다. */
@ConfigurationProperties("spending-guard.integrations.toss-payments")
public record TossPaymentProperties(
        boolean enabled,
        URI baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        String clientKey,
        String secretKey,
        String merchantId,
        String userId
) {
    private static final URI DEFAULT_BASE_URL = URI.create("https://api.tosspayments.com");
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(5);

    public TossPaymentProperties {
        baseUrl = baseUrl == null ? DEFAULT_BASE_URL : baseUrl;
        connectTimeout = connectTimeout == null ? DEFAULT_CONNECT_TIMEOUT : connectTimeout;
        readTimeout = readTimeout == null ? DEFAULT_READ_TIMEOUT : readTimeout;
    }

    /** 활성화된 연동에 필요한 값을 검증하고 연결된 사용자 ID를 반환한다. */
    public UUID configuredUserId() {
        if (!enabled) {
            throw new IllegalStateException("Toss Payments 연동이 비활성화되어 있습니다.");
        }
        if (!StringUtils.hasText(secretKey)
                || !StringUtils.hasText(merchantId)
                || !StringUtils.hasText(userId)) {
            throw new IllegalStateException("Toss Payments 연동 설정이 완전하지 않습니다.");
        }
        try {
            return UUID.fromString(userId);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Toss Payments 연결 사용자 ID 형식이 올바르지 않습니다.", exception);
        }
    }

    /** 브라우저 결제창 초기화에 사용할 공개 테스트 클라이언트 키를 반환한다. */
    public String configuredClientKey() {
        configuredUserId();
        if (!StringUtils.hasText(clientKey)) {
            throw new IllegalStateException("Toss Payments 클라이언트 키가 설정되지 않았습니다.");
        }
        return clientKey.trim();
    }
}
