package com.joajy.spendingguard.support.observability;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    void keepsSafeIncomingRequestIdInResponseAndMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "client-request-42");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> observedRequestId = new AtomicReference<>();

        filter.doFilter(request, response, (ignoredRequest, ignoredResponse) ->
                observedRequestId.set(MDC.get(RequestCorrelationFilter.MDC_KEY)));

        assertThat(observedRequestId).hasValue("client-request-42");
        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME))
                .isEqualTo("client-request-42");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }

    @Test
    void replacesUnsafeRequestIdAndClearsMdcAfterFailure() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestCorrelationFilter.HEADER_NAME, "unsafe\nforged-log");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThatThrownBy(() -> filter.doFilter(
                request,
                response,
                (ignoredRequest, ignoredResponse) -> {
                    throw new IllegalStateException("failed request");
                }
        )).isInstanceOf(IllegalStateException.class);

        assertThat(response.getHeader(RequestCorrelationFilter.HEADER_NAME))
                .matches("[0-9a-f-]{36}");
        assertThat(MDC.get(RequestCorrelationFilter.MDC_KEY)).isNull();
    }
}
