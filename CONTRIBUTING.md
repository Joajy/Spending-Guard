# Contribution Guide

## 작업 흐름

1. 하나의 브랜치에는 하나의 기능이나 역할만 담습니다.
2. 구현과 관련 테스트를 함께 작성합니다.
3. GitHub Actions 결과를 확인한 뒤 Draft Pull Request로 검토를 요청합니다.
4. 승인 전에는 `main`에 병합하지 않습니다.

## 브랜치 규칙

날짜나 작업 순번 대신 변경 목적이 드러나는 이름을 사용합니다.

```text
chore/backend-bootstrap
feat/transaction-ingestion
feat/outbox-publisher
fix/duplicate-event-handling
```

Draft를 포함해 Pull Request가 열려 있는 동안에는 검토와 CI 보완을 위해 브랜치를 유지합니다. 병합이 끝나면 원격 기능 브랜치를 삭제하고, 로컬 브랜치는 최신 `main`을 받은 뒤 삭제합니다. 병합하지 않고 닫은 브랜치는 후속 작업에 재사용할 이유가 없는지 확인한 뒤 정리합니다.

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
docs(architecture): define spend event processing boundaries
feat(ingestion): persist raw event with transactional outbox
test(ledger): verify idempotent reversal processing
perf(batch): record one-million-row benchmark
```

## 완료 조건

- 요구사항 또는 Issue와 연결되어 있다.
- 관련 자동 테스트가 있다.
- 전체 테스트가 통과한다.
- 공개 API 변경은 OpenAPI 문서에 반영한다.
- 설계 판단이 바뀌면 ADR을 갱신한다.
- 측정값에는 실행 환경과 표본 수를 함께 기록한다.
- 비밀키, 금융 원문, 개인정보를 Git에 포함하지 않는다.
