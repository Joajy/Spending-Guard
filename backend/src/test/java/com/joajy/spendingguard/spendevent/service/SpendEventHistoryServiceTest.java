package com.joajy.spendingguard.spendevent.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.exception.InvalidSpendEventQueryException;
import com.joajy.spendingguard.spendevent.service.exception.SpendEventOwnerNotFoundException;
import com.joajy.spendingguard.spendevent.service.model.SpendEventHistoryQuery;
import com.joajy.spendingguard.spendevent.service.port.outbound.CheckSpendEventOwnerPort;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventHistoryPort;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SpendEventHistoryServiceTest {

    private static final UUID USER_ID = UUID.fromString("3f6d4218-f5a6-48cf-9813-006779108c0d");

    @Mock
    private LoadSpendEventHistoryPort loadPort;

    @Mock
    private CheckSpendEventOwnerPort checkOwnerPort;

    private SpendEventHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SpendEventHistoryService(loadPort, checkOwnerPort);
    }

    @Test
    void returnsNextCursorWhenMoreRowsExist() {
        given(checkOwnerPort.exists(USER_ID)).willReturn(true);
        var first = item("10000000-0000-0000-0000-000000000002", "2026-08-18T03:00:00Z");
        var second = item("10000000-0000-0000-0000-000000000001", "2026-08-18T02:00:00Z");
        given(loadPort.find(org.mockito.ArgumentMatchers.any())).willReturn(List.of(first, second));

        var page = service.list(USER_ID, YearMonth.of(2026, 8), null, null, null, 1);

        assertThat(page.items()).containsExactly(first);
        assertThat(page.hasNext()).isTrue();
        assertThat(page.nextCursor()).isNotBlank();
        var query = captureQuery();
        assertThat(query.from()).isEqualTo(Instant.parse("2026-07-31T15:00:00Z"));
        assertThat(query.until()).isEqualTo(Instant.parse("2026-08-31T15:00:00Z"));
        assertThat(query.limit()).isEqualTo(2);
    }

    @Test
    void decodesCursorAndNormalizesCategory() {
        given(checkOwnerPort.exists(USER_ID)).willReturn(true);
        var cursorItem = item(
                "10000000-0000-0000-0000-000000000003",
                "2026-08-18T01:00:00.123456Z"
        );
        String cursor = SpendEventCursorCodec.encode(cursorItem.transactionAt(), cursorItem.eventId());
        given(loadPort.find(org.mockito.ArgumentMatchers.any())).willReturn(List.of());

        service.list(USER_ID, YearMonth.of(2026, 8), SpendEventStatus.ANALYZING,
                " shopping ", cursor, 20);

        var query = captureQuery();
        assertThat(query.category()).isEqualTo("SHOPPING");
        assertThat(query.cursorTransactionAt()).isEqualTo(cursorItem.transactionAt());
        assertThat(query.cursorEventId()).isEqualTo(cursorItem.eventId());
        assertThat(query.status()).isEqualTo(SpendEventStatus.ANALYZING);
    }

    @Test
    void rejectsInvalidPageSize() {
        given(checkOwnerPort.exists(USER_ID)).willReturn(true);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), null, null, null, 101
        )).isInstanceOf(InvalidSpendEventQueryException.class)
                .hasMessageContaining("size");

        verifyNoInteractions(loadPort);
    }

    @Test
    void rejectsInvalidCategoryAndCursor() {
        given(checkOwnerPort.exists(USER_ID)).willReturn(true);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), null, "ENTERTAINMENT", null, 20
        )).isInstanceOf(InvalidSpendEventQueryException.class)
                .hasMessageContaining("category");
        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), null, null, "not-a-cursor", 20
        )).isInstanceOf(InvalidSpendEventQueryException.class)
                .hasMessageContaining("cursor");

        verifyNoInteractions(loadPort);
    }

    @Test
    void rejectsUnknownOwnerBeforeReadingTransactions() {
        given(checkOwnerPort.exists(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), null, null, null, 20
        )).isInstanceOf(SpendEventOwnerNotFoundException.class);

        verifyNoInteractions(loadPort);
    }

    private SpendEventHistoryQuery captureQuery() {
        var captor = ArgumentCaptor.forClass(SpendEventHistoryQuery.class);
        verify(loadPort).find(captor.capture());
        return captor.getValue();
    }

    private SpendEventHistoryItem item(String id, String transactionAt) {
        return new SpendEventHistoryItem(
                UUID.fromString(id),
                "테스트상점 **,***원 결제",
                SpendEventStatus.ANALYZING,
                Instant.parse(transactionAt),
                new BigDecimal("12800"),
                "PAYMENT",
                "SHOPPING",
                false,
                "LOW"
        );
    }
}
