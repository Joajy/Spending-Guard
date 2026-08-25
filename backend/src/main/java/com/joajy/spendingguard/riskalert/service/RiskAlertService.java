package com.joajy.spendingguard.riskalert.service;

import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;

import com.joajy.spendingguard.account.service.exception.UserAccountNotFoundException;
import com.joajy.spendingguard.analysis.domain.model.RiskLevel;
import com.joajy.spendingguard.riskalert.service.exception.InvalidRiskAlertQueryException;
import com.joajy.spendingguard.riskalert.service.model.RiskAlertQuery;
import com.joajy.spendingguard.riskalert.service.port.RiskAlertQueryPort;
import com.joajy.spendingguard.riskalert.service.result.RiskAlertPage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 월별 중간·높음 위험 소비를 최신순 커서 페이지로 제공한다. */
@Service
public class RiskAlertService {

    private static final ZoneId SERVICE_ZONE = ZoneId.of("Asia/Seoul");

    private final RiskAlertQueryPort queryPort;

    public RiskAlertService(RiskAlertQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Transactional(readOnly = true)
    public RiskAlertPage list(
            UUID userId,
            YearMonth month,
            RiskLevel minimumLevel,
            String cursor,
            int size
    ) {
        if (!queryPort.userExists(userId)) {
            throw new UserAccountNotFoundException();
        }
        if (minimumLevel == RiskLevel.LOW) {
            throw new InvalidRiskAlertQueryException(
                    "minimumLevel은 MEDIUM 또는 HIGH만 사용할 수 있습니다."
            );
        }
        if (size < 1 || size > 100) {
            throw new InvalidRiskAlertQueryException("size는 1 이상 100 이하여야 합니다.");
        }

        var decodedCursor = RiskAlertCursorCodec.decode(cursor);
        var from = month.atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var until = month.plusMonths(1).atDay(1).atStartOfDay(SERVICE_ZONE).toInstant();
        var query = new RiskAlertQuery(
                userId,
                from,
                until,
                minimumLevel,
                decodedCursor == null ? null : decodedCursor.transactionAt(),
                decodedCursor == null ? null : decodedCursor.eventId(),
                size + 1
        );
        var loaded = queryPort.find(query);
        boolean hasNext = loaded.size() > size;
        var items = hasNext ? loaded.subList(0, size) : loaded;
        String nextCursor = hasNext
                ? RiskAlertCursorCodec.encode(
                        items.get(items.size() - 1).transactionAt(),
                        items.get(items.size() - 1).eventId()
                )
                : null;
        return new RiskAlertPage(items, nextCursor, hasNext);
    }
}
