# Contribution Guide

## 승인 흐름

각 일자의 작업은 다음 순서를 지킵니다.

1. 작업 목표, 변경 예정 파일, 완료 조건을 사용자에게 제시한다.
2. 사용자 승인 후 로컬 파일을 변경한다.
3. 자동 테스트와 수동 API 검증을 수행한다.
4. 통과 수, 실패 수, 정확도, 성능 수치와 알려진 제한을 보고한다.
5. 사용자가 결과를 승인한 경우에만 Stage와 Commit을 수행한다.
6. 원격 Push와 Pull Request 생성은 별도 승인 후 수행한다.

## 브랜치 규칙

```text
docs/day01-product-design
chore/day02-backend-bootstrap
feature/day03-ingestion-outbox
feature/day04-kafka-publisher
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
