package com.joajy.spendingguard.analysis.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import com.joajy.spendingguard.analysis.domain.model.FastParseStatus;
import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.analysis.domain.model.SpendCategory;
import com.joajy.spendingguard.analysis.domain.model.TransactionType;
import com.joajy.spendingguard.analysis.domain.policy.SpendRiskClassifier;
import com.joajy.spendingguard.analysis.service.command.CorrectSpendCategoryCommand;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryCorrectionNotFoundException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryNotEditableException;
import com.joajy.spendingguard.analysis.service.exception.SpendCategoryVersionConflictException;
import com.joajy.spendingguard.analysis.service.model.SpendCategoryCorrectionTarget;
import com.joajy.spendingguard.analysis.service.port.outbound.LoadSpendCategoryCorrectionTargetPort;
import com.joajy.spendingguard.analysis.service.port.outbound.StoreSpendCategoryOverridePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SpendCategoryCorrectionServiceTest {

    private static final UUID USER_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID EVENT_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");
    private static final Instant NOW = Instant.parse("2026-08-25T01:00:00Z");

    @Mock
    private LoadSpendCategoryCorrectionTargetPort loadTargetPort;

    @Mock
    private StoreSpendCategoryOverridePort storeOverridePort;

    private SpendCategoryCorrectionService service;

    @BeforeEach
    void setUp() {
        service = new SpendCategoryCorrectionService(
                loadTargetPort,
                storeOverridePort,
                new SpendRiskClassifier(),
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void storesOverrideAndRecalculatesDerivedRisk() {
        var target = target(FastParseStatus.PARSED, 0);
        given(loadTargetPort.find(USER_ID, EVENT_ID)).willReturn(Optional.of(target));
        given(storeOverridePort.store(
                org.mockito.ArgumentMatchers.eq(USER_ID),
                org.mockito.ArgumentMatchers.eq(EVENT_ID),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.eq(0L),
                org.mockito.ArgumentMatchers.eq(NOW)
        )).willReturn(OptionalLong.of(1));

        var result = service.correct(new CorrectSpendCategoryCommand(
                USER_ID, EVENT_ID, SpendCategory.DELIVERY, 0
        ));

        assertThat(result.originalCategory()).isEqualTo(SpendCategory.SHOPPING);
        assertThat(result.category()).isEqualTo(SpendCategory.DELIVERY);
        assertThat(result.fixedCost()).isFalse();
        assertThat(result.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.riskReason()).isEqualTo("HIGH_DELIVERY_AMOUNT");
        assertThat(result.version()).isEqualTo(1);
        assertThat(result.correctedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsEventBeforeParsingCompletes() {
        given(loadTargetPort.find(USER_ID, EVENT_ID))
                .willReturn(Optional.of(target(null, 0)));

        assertThatThrownBy(() -> service.correct(new CorrectSpendCategoryCommand(
                USER_ID, EVENT_ID, SpendCategory.OTHER, 0
        ))).isInstanceOf(SpendCategoryNotEditableException.class);

        verifyNoInteractions(storeOverridePort);
    }

    @Test
    void rejectsStaleVersionBeforeWriting() {
        given(loadTargetPort.find(USER_ID, EVENT_ID))
                .willReturn(Optional.of(target(FastParseStatus.PARSED, 2)));

        assertThatThrownBy(() -> service.correct(new CorrectSpendCategoryCommand(
                USER_ID, EVENT_ID, SpendCategory.OTHER, 1
        ))).isInstanceOf(SpendCategoryVersionConflictException.class);

        verifyNoInteractions(storeOverridePort);
    }

    @Test
    void rejectsConcurrentVersionChangeDetectedByDatabase() {
        given(loadTargetPort.find(USER_ID, EVENT_ID))
                .willReturn(Optional.of(target(FastParseStatus.PARSED, 1)));
        given(storeOverridePort.store(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any()
        )).willReturn(OptionalLong.empty());

        assertThatThrownBy(() -> service.correct(new CorrectSpendCategoryCommand(
                USER_ID, EVENT_ID, SpendCategory.OTHER, 1
        ))).isInstanceOf(SpendCategoryVersionConflictException.class);
    }

    @Test
    void hidesUnknownOrOtherUsersEventAsNotFound() {
        given(loadTargetPort.find(USER_ID, EVENT_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.correct(new CorrectSpendCategoryCommand(
                USER_ID, EVENT_ID, SpendCategory.OTHER, 0
        ))).isInstanceOf(SpendCategoryCorrectionNotFoundException.class);

        verifyNoInteractions(storeOverridePort);
    }

    private SpendCategoryCorrectionTarget target(FastParseStatus status, long version) {
        return new SpendCategoryCorrectionTarget(
                EVENT_ID,
                status,
                status == null ? null : SpendCategory.SHOPPING,
                status == null ? null : new BigDecimal("60000"),
                status == null ? null : TransactionType.PAYMENT,
                Instant.parse("2026-08-25T00:30:00Z"),
                version
        );
    }
}
