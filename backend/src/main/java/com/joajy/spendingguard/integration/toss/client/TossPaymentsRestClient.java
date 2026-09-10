package com.joajy.spendingguard.integration.toss.client;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import com.joajy.spendingguard.integration.toss.config.TossPaymentProperties;
import com.joajy.spendingguard.integration.toss.service.TossPaymentVerificationException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Toss Payments 결제 조회 API를 호출해 웹훅의 paymentKey를 검증한다. */
@Component
class TossPaymentsRestClient implements TossPaymentClient {

    private final RestClient restClient;
    private final TossPaymentProperties properties;

    TossPaymentsRestClient(RestClient.Builder builder, TossPaymentProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .build();
        this.properties = properties;
    }

    @Override
    public Payment getPayment(String paymentKey) {
        try {
            Payment payment = restClient.get()
                    .uri("/v1/payments/{paymentKey}", paymentKey)
                    .headers(headers -> headers.setBasicAuth(
                            properties.secretKey(),
                            "",
                            StandardCharsets.UTF_8
                    ))
                    .retrieve()
                    .body(Payment.class);
            if (payment == null) {
                throw new TossPaymentVerificationException("Toss Payments 결제 조회 결과가 비어 있습니다.");
            }
            return payment;
        } catch (RestClientException exception) {
            throw new TossPaymentVerificationException("Toss Payments 결제를 확인할 수 없습니다.", exception);
        }
    }

    @Override
    public Payment confirmPayment(String paymentKey, String orderId, long amount) {
        try {
            Payment payment = restClient.post()
                    .uri("/v1/payments/confirm")
                    .headers(headers -> headers.setBasicAuth(
                            properties.secretKey(),
                            "",
                            StandardCharsets.UTF_8
                    ))
                    .body(new ConfirmPaymentRequest(paymentKey, orderId, amount))
                    .retrieve()
                    .body(Payment.class);
            if (payment == null) {
                throw new TossPaymentVerificationException("Toss Payments 결제 승인 결과가 비어 있습니다.");
            }
            return payment;
        } catch (RestClientException exception) {
            throw new TossPaymentVerificationException("Toss Payments 결제를 승인할 수 없습니다.", exception);
        }
    }

    @Override
    public Payment cancelPayment(String paymentKey, String reason) {
        try {
            Payment payment = restClient.post()
                    .uri("/v1/payments/{paymentKey}/cancel", paymentKey)
                    .headers(headers -> headers.setBasicAuth(
                            properties.secretKey(),
                            "",
                            StandardCharsets.UTF_8
                    ))
                    .body(new CancelPaymentRequest(reason))
                    .retrieve()
                    .body(Payment.class);
            if (payment == null) {
                throw new TossPaymentVerificationException("Toss Payments 결제 취소 결과가 비어 있습니다.");
            }
            return payment;
        } catch (RestClientException exception) {
            throw new TossPaymentVerificationException("Toss Payments 결제를 취소할 수 없습니다.", exception);
        }
    }

    private record ConfirmPaymentRequest(String paymentKey, String orderId, long amount) {
    }

    private record CancelPaymentRequest(String cancelReason) {
    }
}
