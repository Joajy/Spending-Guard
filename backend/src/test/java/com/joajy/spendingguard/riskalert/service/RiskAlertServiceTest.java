package com.joajy.spendingguard.riskalert.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.exception.InvalidRiskAlertQueryException;
import com.joajy.spendingguard.riskalert.service.model.RiskAlertQuery;
import com.joajy.spendingguard.riskalert.service.port.RiskAlertQueryPort;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertItem;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RiskAlertServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private final RiskAlertQueryPort queryPort = mock(RiskAlertQueryPort.class);
    private final RiskAlertService service = new RiskAlertService(queryPort);

    @Test
    void returnsCursorPageAndLoadsOneExtraItem() {
        var first = alert(Instant.parse("2026-08-20T01:00:00Z"), RiskLevel.HIGH);
        var second = alert(Instant.parse("2026-08-19T01:00:00Z"), RiskLevel.MEDIUM);
        var extra = alert(Instant.parse("2026-08-18T01:00:00Z"), RiskLevel.MEDIUM);
        given(queryPort.userExists(USER_ID)).willReturn(true);
        given(queryPort.find(org.mockito.ArgumentMatchers.any())).willReturn(
                List.of(first, second, extra)
        );

        var result = service.list(
                USER_ID, YearMonth.of(2026, 8), RiskLevel.MEDIUM, null, 2
        );

        assertThat(result.items()).containsExactly(first, second);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.nextCursor()).isNotBlank();
        var queryCaptor = ArgumentCaptor.forClass(RiskAlertQuery.class);
        verify(queryPort).find(queryCaptor.capture());
        assertThat(queryCaptor.getValue().limit()).isEqualTo(3);
        assertThat(queryCaptor.getValue().from()).isEqualTo("2026-07-31T15:00:00Z");
        assertThat(queryCaptor.getValue().until()).isEqualTo("2026-08-31T15:00:00Z");
    }

    @Test
    void rejectsLowRiskAndInvalidPageSize() {
        given(queryPort.userExists(USER_ID)).willReturn(true);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), RiskLevel.LOW, null, 20
        )).isInstanceOf(InvalidRiskAlertQueryException.class);
        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), RiskLevel.MEDIUM, null, 101
        )).isInstanceOf(InvalidRiskAlertQueryException.class);
    }

    @Test
    void rejectsUnknownUserBeforeQueryingAlerts() {
        given(queryPort.userExists(USER_ID)).willReturn(false);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), RiskLevel.MEDIUM, null, 20
        )).isInstanceOf(UserAccountNotFoundException.class);
    }

    @Test
    void rejectsMalformedCursor() {
        given(queryPort.userExists(USER_ID)).willReturn(true);

        assertThatThrownBy(() -> service.list(
                USER_ID, YearMonth.of(2026, 8), RiskLevel.MEDIUM, "broken", 20
        )).isInstanceOf(InvalidRiskAlertQueryException.class);
    }

    private RiskAlertItem alert(Instant transactionAt, RiskLevel riskLevel) {
        return new RiskAlertItem(
                UUID.randomUUID(),
                "소비 알림",
                transactionAt,
                BigDecimal.valueOf(28_000),
                "TRANSPORT",
                riskLevel,
                "LATE_NIGHT_TRANSPORT",
                0
        );
    }
}
