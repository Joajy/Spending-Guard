# ADR-0002: Guarantee Ledger Consistency with PostgreSQL, Not Redis Locks

- Status: Accepted for Day 1 review
- Date: 2026-08-11

## Context

동일 사용자의 소비 이벤트가 동시에 처리되거나 Kafka에서 재전달될 수 있다. 원장과 예산 Projection에 중복 또는 Lost Update가 생기면 안 된다.

## Decision

PostgreSQL을 정합성의 최종 기준으로 사용한다.

1. Raw Event, Outbox, Ledger, 처리기록에 유니크 제약을 둔다.
2. Consumer는 `processed_event`를 이용해 멱등하게 처리한다.
3. 원장은 append-only이며 취소·환불은 음수 보상 항목으로 기록한다.
4. 예산 Projection은 `spent_amount = spent_amount + :amount` 원자적 UPDATE를 사용한다.
5. 처리기록, 원장, Projection, 다음 Outbox를 하나의 DB 트랜잭션에 묶는다.
6. Kafka Offset은 DB Commit 이후에 Commit한다.
7. 기본 격리 수준은 `READ COMMITTED`로 시작하고, 불변조건별 동시성 테스트가 더 강한 잠금을 요구할 때만 `SELECT FOR UPDATE` 또는 조건부 UPDATE를 사용한다.
8. 원장 합계와 Projection을 주기적으로 대조한다.

## Why Not Redis Lock

- Redis 락과 PostgreSQL Commit은 하나의 원자적 트랜잭션이 아니다.
- 네트워크 단절, lease 만료, 프로세스 중단 시 락 소유권과 DB 상태가 어긋날 수 있다.
- 수정 대상 데이터가 PostgreSQL에 있으므로 DB 제약과 행 잠금이 더 직접적인 정합성 수단이다.
- MVP의 처리량에서는 분산 락을 추가할 근거가 없다.

## Validation

- 같은 eventId 10,000회 처리 후 원장 1건
- 같은 사용자에 결제 100건 동시 처리 후 합계 오차 0원
- DB Commit 후 Offset Commit 전 장애를 재현하고 중복 반영 0건
- 취소 이벤트 반복 전달 후 보상 항목 1건
- 원장 재집계와 Projection 불일치 0건

## Revisit Trigger

Redis는 조회 캐시 또는 Rate Limit 목적으로만, 성능 측정이 필요성을 입증한 후 검토한다.
