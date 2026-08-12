package com.joajy.spendingguard.spendevent.application;

import java.time.Instant;

import com.joajy.spendingguard.outbox.OutboxEvent;
import com.joajy.spendingguard.outbox.OutboxEventRepository;
import com.joajy.spendingguard.spendevent.domain.SpendEventSource;
import com.joajy.spendingguard.spendevent.persistence.RawSpendEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class SpendEventTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private SpendEventService spendEventService;

    @Autowired
    private RawSpendEventRepository rawSpendEventRepository;

    @MockitoBean
    private OutboxEventRepository outboxEventRepository;

    @BeforeEach
    void cleanDatabase() {
        rawSpendEventRepository.deleteAll();
    }

    @Test
    void rollsBackRawEventWhenOutboxStorageFails() {
        given(outboxEventRepository.save(any(OutboxEvent.class)))
                .willThrow(new IllegalStateException("outbox unavailable"));
        SubmitSpendEventCommand command = new SubmitSpendEventCommand(
                SpendEventSource.SIMULATOR,
                "rollback-check-100",
                "테스트상점 12,800원 결제",
                Instant.parse("2026-08-13T01:00:00Z")
        );

        assertThatThrownBy(() -> spendEventService.submit(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("outbox unavailable");
        assertThat(rawSpendEventRepository.count()).isZero();
    }
}
