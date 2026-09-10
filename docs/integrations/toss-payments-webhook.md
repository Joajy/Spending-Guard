# Toss Payments 결제·취소 웹훅

이 연동은 **연결한 Toss Payments 상점에서 발생한 결제와 취소**를 Spending Guard의
기존 Outbox/Kafka 분석 파이프라인으로 자동 접수한다. 개인이 토스 앱이나 카드로 결제한
전체 내역을 가져오는 기능은 아니다.

## 현재 검증 범위

- `PAYMENT_STATUS_CHANGED` 웹훅을 받는다.
- 웹훅 본문의 금액과 상점 정보는 신뢰하지 않는다. `paymentKey`로 Toss Payments 결제
  조회 API를 호출해 원본 Payment 객체를 다시 읽는다.
- 조회 결과의 `paymentKey`와 `mId`가 설정값과 일치할 때만 접수한다.
- 승인 결제는 `paymentKey`, 완료된 각 취소는 Toss의 `transactionKey`를 외부 식별자로
  사용한다. 같은 웹훅이 재전송되어도 DB 고유 제약으로 한 번만 처리한다.
- 결제는 예산 사용액을 늘리고, 취소는 취소가 발생한 월의 사용액을 줄이는 현금흐름
  정책을 사용한다. 결제·취소 이벤트 순서가 바뀌어도 합산 결과는 같다.

일반 결제 상태 웹훅에는 Toss가 검증 가능한 서명 헤더를 제공하지 않는다. 따라서 공개
엔드포인트를 열되 Toss 조회 API 재검증을 필수로 한다. 조회 실패나 상점 불일치는 200을
반환하지 않아 Toss의 재전송 대상이 된다.

## 설정

연동할 테스트 상점 하나와 Spending Guard 사용자 하나를 연결한다.

```text
TOSS_PAYMENT_WEBHOOK_ENABLED=true
TOSS_PAYMENT_SECRET_KEY=test_sk_...
TOSS_PAYMENT_MERCHANT_ID=your_test_mid
TOSS_PAYMENT_USER_ID=Spending Guard 사용자 UUID
TOSS_PAYMENT_CONNECT_TIMEOUT=2s
TOSS_PAYMENT_READ_TIMEOUT=5s
```

비밀 키는 저장소에 커밋하지 않는다. 운영 환경의 Secret 저장소에서 주입한다.

Toss Payments 개발자센터에서 다음 URL을 `PAYMENT_STATUS_CHANGED` 웹훅으로 등록한다.

```text
https://{public-api-host}/api/v1/integrations/toss-payments/webhook
```

로컬에서는 HTTPS 터널을 사용해 `localhost:8080`을 노출하거나, 아래 형식으로 컨트롤러와
파이프라인만 먼저 점검할 수 있다. 실제 접수 시 서버는 `paymentKey`를 Toss API에서 조회한다.

```http
POST /api/v1/integrations/toss-payments/webhook HTTP/1.1
Content-Type: application/json

{
  "eventType": "PAYMENT_STATUS_CHANGED",
  "data": {
    "paymentKey": "test_payment_key"
  }
}
```

정상 응답은 새로 접수한 이벤트 수와 이미 처리한 이벤트 수를 반환한다.

```json
{
  "ignored": false,
  "acceptedCount": 2,
  "duplicateCount": 0
}
```

## 현재 한계와 다음 단계

- 테스트 상점 1개를 사용자 1명에게 연결하는 포트폴리오 단계다. 다중 사용자를 지원하려면
  상점 연결 테이블, OAuth/키 암호화, 연결 해제 흐름이 필요하다.
- 취소는 취소 발생 월의 음수 소비로 반영한다. 원 결제 월을 수정하는 회계 정책이 필요하면
  결제-취소 관계를 별도 원장 필드로 저장해야 한다.
- 공개 웹훅의 트래픽 남용 방지는 배포 경계의 rate limit/WAF로 보완한다.
