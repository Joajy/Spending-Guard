package com.joajy.spendingguard.support.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청과 응답, 서버 로그를 같은 식별자로 연결한다.
 *
 * <p>외부 요청 식별자는 로그 위조를 막기 위해 제한된 문자와 길이만 허용한다. 값이 없거나
 * 형식이 맞지 않으면 서버가 새 UUID를 만들며, 요청 처리가 끝나면 스레드 재사용에 식별자가
 * 남지 않도록 MDC를 정리한다.
 */
@Component
public class RequestCorrelationFilter extends OncePerRequestFilter {

    static final String HEADER_NAME = "X-Request-ID";
    static final String MDC_KEY = "requestId";
    private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = normalizedRequestId(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER_NAME, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private String normalizedRequestId(String candidate) {
        if (candidate != null && SAFE_REQUEST_ID.matcher(candidate).matches()) {
            return candidate;
        }
        return UUID.randomUUID().toString();
    }
}
