# Contribution Guide

## 작업 흐름

1. 작업 목표와 완료 조건을 정한다.
2. 작업 성격에 맞는 브랜치를 만든다.
3. 기능 코드와 관련 테스트 코드를 함께 작성한다.
4. 자동 테스트를 실행하고 역할 기반 브랜치에 커밋한다.
5. Draft PR에서 변경 내용과 실제 테스트 결과를 확인한다.
6. 사용자 승인 후에만 `main`에 병합한다.

## 브랜치 규칙

브랜치명은 날짜가 아니라 기능 또는 역할을 나타낸다.

```text
chore/backend-bootstrap
feat/transaction-ingestion
feat/budget-ledger
feat/risk-detection
fix/duplicate-event-handling
```

## 커밋 규칙

Conventional Commits 형식을 사용합니다.

```text
<type>(<scope>): <subject>
```

허용하는 주요 type:

- `feat`: 사용자 기능
- `fix`: 결함 수정
- `test`: 테스트 추가·변경
- `perf`: 성능 개선 또는 벤치마크
- `docs`: 문서
- `refactor`: 동작을 바꾸지 않는 구조 변경
- `build`: 빌드와 의존성
- `ci`: CI 설정
- `chore`: 그 밖의 유지보수

예시:

```text
feat(ingestion): persist raw event with transactional outbox
test(ledger): verify idempotent reversal processing
perf(batch): record one-million-row benchmark
```

## 완료 조건

- 요구사항 또는 Issue와 연결되어 있다.
- 기능과 관련된 단위 테스트 또는 통합 테스트가 있다.
- 전체 테스트가 통과한다.
- 공개 API 변경은 OpenAPI 문서에 반영한다.
- 설계 판단이 바뀌면 ADR을 갱신한다.
- 측정값에는 실행 환경과 표본 수를 함께 기록한다.
- 비밀키, 금융 원문, 개인정보를 Git에 포함하지 않는다.
