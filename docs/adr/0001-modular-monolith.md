# ADR-0001: Start as a Modular Monolith

- Status: Accepted for Day 1 review
- Date: 2026-08-11

## Context

백엔드 MVP 기간은 2주이고 한 명이 개발한다. Kafka 기반 비동기 처리와 API·Worker 분리 가능성은 보여줘야 하지만, 여러 독립 서비스의 배포·계약·관측 복잡성은 MVP 가치보다 크다.

## Decision

- 하나의 Spring Boot 코드베이스를 사용한다.
- package-by-feature로 모듈 경계를 만든다.
- API와 Worker는 실행 프로필로 분리할 수 있게 한다.
- 로컬에서는 하나의 프로세스로 실행 가능하게 한다.
- 모듈 간 내부 Repository 직접 접근을 제한하고 공개 Application API 또는 이벤트를 사용한다.
- 구조 검증은 ArchUnit 또는 Spring Modulith 검사를 사용한다.

## Consequences

### Positive

- 2주 내 기능과 검증에 집중할 수 있다.
- 트랜잭션 경계를 명확히 유지한다.
- Worker만 독립 확장하는 진화 경로를 설명할 수 있다.
- 테스트와 로컬 실행이 단순하다.

### Negative

- 코드베이스가 하나이므로 물리적 격리가 없다.
- 잘못된 의존성을 자동 검사하지 않으면 모듈 경계가 약해질 수 있다.

## Revisit Trigger

- Worker 부하가 API와 독립적으로 확장되어야 한다.
- 배포 주기 또는 장애 경계를 물리적으로 분리할 필요가 측정된다.
- 특정 모듈의 데이터 소유권이 독립 DB를 요구한다.
