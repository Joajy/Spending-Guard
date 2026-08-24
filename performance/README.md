# 소비 이벤트 접수 성능 기준선

소비 알림을 받는 `POST /api/v1/users/{userId}/spend-events`의 응답 성능을 같은 조건에서 반복 측정한다. 이 API는 원천 이벤트와 Outbox를 하나의 트랜잭션으로 저장한 뒤 `202 Accepted`를 반환한다.

## 기본 시나리오

- 동시 사용자: 10명
- 사용자별 요청: 20건
- 증가 시간: 10초
- 전체 요청: 200건
- 성공 조건: 모든 응답이 `202`이고 본문 상태가 `RECEIVED`
- 기준: 오류율 0%, p95 응답 시간 1,000ms 이하

GitHub Actions의 PostgreSQL과 API를 사용하며, Kafka 발행기와 분석 Consumer는 끈다. 따라서 이 결과는 **접수 트랜잭션의 기준선**이며 Kafka를 포함한 전체 파이프라인 처리량이나 운영 SLA를 의미하지 않는다.

## 결과 확인

Pull Request의 `Transaction ingestion performance` 실행에서 다음 자료를 내려받을 수 있다.

- `summary.json`: 요청 수, 오류율, 처리량, 평균, p50, p95, p99
- `results.jtl`: 요청별 원본 측정값
- `html/index.html`: JMeter 차트와 구간별 통계
- `application.log`: 측정 당시 API 로그

실행 요약에는 핵심 수치와 기준 충족 여부가 함께 표시된다. 공유 실행 환경의 편차가 있으므로 단일 수치를 최대 처리량으로 해석하지 않고, 같은 설정의 반복 실행과 변경 전후 비교에 사용한다.

## 로컬 실행

Java 21, PostgreSQL, Apache JMeter 5.6.3이 필요하다. API를 실행하고 이메일 인증을 마친 테스트 사용자의 토큰과 ID를 준비한 뒤 아래 명령을 실행한다.

```shell
jmeter -n \
  -t performance/jmeter/transaction-ingestion.jmx \
  -l performance/results/results.jtl \
  -e -o performance/results/html \
  -JaccessToken=<access-token> \
  -JuserId=<user-id> \
  -Jthreads=10 \
  -Jiterations=20 \
  -JrampSeconds=10
```

측정 전 `performance/results`를 비워야 하며, 결과 파일은 저장소에 커밋하지 않는다.
