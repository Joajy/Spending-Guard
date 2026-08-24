package com.joajy.spendingguard.spendevent.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import com.joajy.spendingguard.spendevent.domain.model.SpendEventStatus;
import com.joajy.spendingguard.spendevent.service.exception.InvalidSpendEventQueryException;
import com.joajy.spendingguard.spendevent.service.exception.SpendEventOwnerNotFoundException;
import com.joajy.spendingguard.spendevent.service.model.SpendEventHistoryQuery;
import com.joajy.spendingguard.spendevent.service.port.inbound.ListSpendEventsUseCase;
import com.joajy.spendingguard.spendevent.service.port.outbound.CheckSpendEventOwnerPort;
import com.joajy.spendingguard.spendevent.service.port.outbound.LoadSpendEventHistoryPort;
import com.joajy.spendingguard.spendevent.service.result.SpendEventHistoryPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 월별 소비 내역을 필터링하고 커서 기반 페이지로 반환한다.
 *
 * <p>서비스 기준 월은 {@code Asia/Seoul}의 반개구간 {@code [월 시작, 다음 달 시작)}으로
 * 계산한다. 페이지마다 요청 크기보다 한 행 더 읽어 다음 페이지 존재 여부를 판단하고,
 * 마지막 거래의 시각과 ID를 커서로 만들어 데이터가 추가되어도 순서가 흔들리지 않게 한다.
 */
@Service
public class SpendEventHistoryService implements ListSpendEventsUseCase {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");
    private static final Set<String> CATEGORIES = Set.of(
            "SUBSCRIPTION", "TRANSPORT", "DELIVERY", "SHOPPING", "TRANSFER", "OTHER"
    );

    private final LoadSpendEventHistoryPort loadPort;
    private final CheckSpendEventOwnerPort checkOwnerPort;

    public SpendEventHistoryService(
            LoadSpendEventHistoryPort loadPort,
            CheckSpendEventOwnerPort checkOwnerPort
    ) {
        this.loadPort = loadPort;
        this.checkOwnerPort = checkOwnerPort;
    }

    @Override
    @Transactional(readOnly = true)
    public SpendEventHistoryPage list(
            UUID userId,
            YearMonth month,
            SpendEventStatus status,
            String category,
            String cursor,
            int size
    ) {
        if (!checkOwnerPort.exists(userId)) {
            throw new SpendEventOwnerNotFoundException();
        }
        if (size < 1 || size > 100) {
            throw new InvalidSpendEventQueryException("size는 1 이상 100 이하여야 합니다.");
        }
        String normalizedCategory = normalizeCategory(category);
        var decodedCursor = SpendEventCursorCodec.decode(cursor);
        var from = month.atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var until = month.plusMonths(1).atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var query = new SpendEventHistoryQuery(
                userId,
                from,
                until,
                status,
                normalizedCategory,
                decodedCursor == null ? null : decodedCursor.transactionAt(),
                decodedCursor == null ? null : decodedCursor.eventId(),
                size + 1
        );
        var loaded = loadPort.find(query);
        boolean hasNext = loaded.size() > size;
        var items = hasNext ? loaded.subList(0, size) : loaded;
        String nextCursor = hasNext
                ? SpendEventCursorCodec.encode(
                        items.get(items.size() - 1).transactionAt(),
                        items.get(items.size() - 1).eventId()
                )
                : null;
        return new SpendEventHistoryPage(items, nextCursor, hasNext);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        String normalized = category.strip().toUpperCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) {
            throw new InvalidSpendEventQueryException("category 값을 확인해 주세요.");
        }
        return normalized;
    }
}
