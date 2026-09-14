package com.joajy.spendingguard.integration.toss.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import com.joajy.spendingguard.integration.toss.service.model.TossTestOrder;
import com.joajy.spendingguard.integration.toss.service.port.outbound.TossTestOrderPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** PostgreSQL 원장을 이용해 동일 테스트 주문의 중복 승인을 선점한다. */
@Component
class TossTestOrderPersistenceAdapter implements TossTestOrderPort {

    private static final String CLAIM_ORDER = """
            UPDATE toss_test_order
               SET status = 'CONFIRMING', updated_at = :claimedAt
             WHERE order_id = :orderId
               AND user_id = :userId
               AND amount = :amount
               AND status = 'PENDING'
            RETURNING order_id, user_id, amount, order_name, created_at
            """;

    private final JdbcClient jdbcClient;

    TossTestOrderPersistenceAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void create(TossTestOrder order) {
        jdbcClient.sql("""
                INSERT INTO toss_test_order
                    (order_id, user_id, amount, order_name, status, created_at, updated_at)
                VALUES (:orderId, :userId, :amount, :orderName, 'PENDING', :createdAt, :createdAt)
                """)
                .param("orderId", order.orderId())
                .param("userId", order.userId())
                .param("amount", order.amount())
                .param("orderName", order.orderName())
                .param("createdAt", order.createdAt().atOffset(ZoneOffset.UTC))
                .update();
    }

    @Override
    public Optional<TossTestOrder> claimForConfirmation(
            UUID userId,
            String orderId,
            long amount,
            Instant claimedAt
    ) {
        return jdbcClient.sql(CLAIM_ORDER)
                .param("userId", userId)
                .param("orderId", orderId)
                .param("amount", amount)
                .param("claimedAt", claimedAt.atOffset(ZoneOffset.UTC))
                .query((row, rowNumber) -> new TossTestOrder(
                        row.getString("order_id"),
                        row.getObject("user_id", UUID.class),
                        row.getLong("amount"),
                        row.getString("order_name"),
                        row.getObject("created_at", java.time.OffsetDateTime.class).toInstant()
                ))
                .optional();
    }

    @Override
    public void markConfirmed(String orderId, String paymentKey, Instant confirmedAt) {
        int updated = jdbcClient.sql("""
                UPDATE toss_test_order
                   SET status = 'CONFIRMED', payment_key = :paymentKey,
                       confirmed_at = :confirmedAt, updated_at = :confirmedAt
                 WHERE order_id = :orderId AND status = 'CONFIRMING'
                """)
                .param("orderId", orderId)
                .param("paymentKey", paymentKey)
                .param("confirmedAt", confirmedAt.atOffset(ZoneOffset.UTC))
                .update();
        if (updated != 1) {
            throw new IllegalStateException("Toss 테스트 주문 승인 상태를 저장할 수 없습니다.");
        }
    }

    @Override
    public void releaseConfirmation(String orderId) {
        jdbcClient.sql("""
                UPDATE toss_test_order SET status = 'PENDING', updated_at = CURRENT_TIMESTAMP
                 WHERE order_id = :orderId AND status = 'CONFIRMING'
                """)
                .param("orderId", orderId)
                .update();
    }

    @Override
    public Optional<TossTestOrder> claimForCancellation(
            UUID userId,
            String orderId,
            String paymentKey,
            Instant claimedAt
    ) {
        return jdbcClient.sql("""
                UPDATE toss_test_order
                   SET status = 'CANCELING', updated_at = :claimedAt
                 WHERE order_id = :orderId AND user_id = :userId
                   AND payment_key = :paymentKey AND status = 'CONFIRMED'
                RETURNING order_id, user_id, amount, order_name, created_at
                """)
                .param("orderId", orderId)
                .param("userId", userId)
                .param("paymentKey", paymentKey)
                .param("claimedAt", claimedAt.atOffset(ZoneOffset.UTC))
                .query((row, rowNumber) -> new TossTestOrder(
                        row.getString("order_id"),
                        row.getObject("user_id", UUID.class),
                        row.getLong("amount"),
                        row.getString("order_name"),
                        row.getObject("created_at", java.time.OffsetDateTime.class).toInstant()
                ))
                .optional();
    }

    @Override
    public void markCanceled(String orderId, Instant canceledAt) {
        int updated = jdbcClient.sql("""
                UPDATE toss_test_order
                   SET status = 'CANCELED', updated_at = :canceledAt
                 WHERE order_id = :orderId AND status = 'CANCELING'
                """)
                .param("orderId", orderId)
                .param("canceledAt", canceledAt.atOffset(ZoneOffset.UTC))
                .update();
        if (updated != 1) {
            throw new IllegalStateException("Toss 테스트 주문 취소 상태를 저장할 수 없습니다.");
        }
    }

    @Override
    public void releaseCancellation(String orderId) {
        jdbcClient.sql("""
                UPDATE toss_test_order SET status = 'CONFIRMED', updated_at = CURRENT_TIMESTAMP
                 WHERE order_id = :orderId AND status = 'CANCELING'
                """)
                .param("orderId", orderId)
                .update();
    }
}
