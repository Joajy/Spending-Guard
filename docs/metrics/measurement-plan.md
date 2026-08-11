# Measurement Plan

## 1. 원칙

- 목표값과 실제값을 구분한다.
- 표본 수, 실행 환경, 데이터 버전, 코드 Commit을 함께 기록한다.
- 실패 결과를 삭제하지 않는다.
- 테스트 통과율, 코드 커버리지, AI 정확도, 성능을 혼합하지 않는다.
- 98% 같은 수치는 실제 평가가 그 값에 도달했을 때만 사용한다.

## 2. 기능 테스트

| 지표 | 계산식 | 목표 |
|---|---|---:|
| Test Execution Rate | 실행 테스트 / 계획 테스트 | 100% |
| Pass Rate | 통과 테스트 / 실행 테스트 | 100% |
| Line Coverage | 실행된 Line / 전체 Line | 80% 이상 |
| Branch Coverage | 실행된 Branch / 전체 Branch | 70% 이상 |

도구 후보: JUnit, JaCoCo, Testcontainers, Postman/Newman.

## 3. Parsing·AI 정확도

최종 평가 데이터는 구현 전에 라벨을 고정하고, 프롬프트·규칙 조정용 데이터와 템플릿이 겹치지 않도록 분리한다.

| 필드 | 지표 | 목표 |
|---|---|---:|
| 금액 | Exact Match | 99% 이상 |
| 승인·취소·환불 | Accuracy, Macro F1 | 98% 이상 |
| 발생 시각 | 허용오차 내 Exact Match | 98% 이상 |
| 가맹점 | Normalized Exact Match | 95% 이상 |
| 카테고리 | Macro F1 | 90% 이상 |
| AI 구조화 출력 | Schema-valid Response Rate | 재시도 포함 99% 이상 |

보고서에는 Confusion Matrix와 95% 신뢰구간을 포함한다.

## 4. Risk Engine

Risk Engine은 결정론적 규칙이므로 Accuracy 대신 요구사항 적합률을 측정한다.

| 시나리오 | 기대 결과 |
|---|---|
| 예산 80% 이상 소진 | BUDGET_THRESHOLD Signal |
| 월 진행률 대비 1.5배 빠른 소진 | BUDGET_BURN_RATE Signal |
| 최근 7일 소비가 과거 평균의 1.8배 | WEEKLY_SPEND_SPIKE Signal |
| 동일 가맹점·금액 3분 내 반복 | POSSIBLE_DUPLICATE Signal |
| 반복 결제 금액 20% 이상 증가 | RECURRING_AMOUNT_DRIFT Signal |

목표: 정의된 경계값·정상·이상 시나리오 적합률 100%.

## 5. 정합성과 신뢰성

| 시험 | 목표 |
|---|---:|
| 동일 이벤트 10,000회 재전달 | 원장 중복 0건 |
| 동일 사용자 결제 100건 동시 처리 | 합계 오차 0원 |
| Kafka 중단 중 API 수집 | Raw Event 유실 0건 |
| Worker 강제 종료 후 재시작 | 최종 처리 유실 0건 |
| 취소 웹훅 반복 전달 | 보상 원장 중복 0건 |
| 원장·Projection 대조 | 불일치 0건 |

## 6. 성능

측정 전 하드웨어, JVM, DB, Kafka, Partition, 데이터 크기를 기록한다.

| 지표 | 초기 목표 |
|---|---:|
| 수집 API p95 | 200ms 이하 |
| 수집 API 오류율 | 1% 미만 |
| 비동기 처리 완료 p95 | 외부 AI 포함 10초 이하 |
| Kafka Consumer Lag | 정상 부하 종료 후 60초 내 0 |

목표 미달은 실패로 기록하고 원인을 분석한다. 숫자를 맞추기 위해 테스트 조건을 임의로 낮추지 않는다.

## 7. 일별 보고서 형식

```text
자동 테스트: passed / executed
Postman: passed / assertions
Line Coverage:
Branch Coverage:
Accuracy/F1: 표본 수와 함께 기재
p50/p95/p99:
오류율:
유실·중복:
알려진 제한:
```
