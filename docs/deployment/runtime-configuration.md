# 운영 환경 설정

운영 배포는 `prod` Spring profile과 Next.js production build를 사용한다. 로컬 기본값이 운영 환경에 섞이지 않도록 연결 정보와 비밀값은 모두 실행 환경에서 주입한다.

## 필수 환경 변수

백엔드는 다음 값을 요구한다.

| 구분 | 환경 변수 |
|---|---|
| PostgreSQL | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` |
| Kafka | `KAFKA_BOOTSTRAP_SERVERS` |
| JWT | `JWT_ISSUER`, `JWT_SECRET` |
| SMTP | `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SMTP_AUTH`, `MAIL_STARTTLS_ENABLE`, `MAIL_FROM` |

`JWT_SECRET`은 32바이트 이상이어야 한다. 운영 실행에서는 로컬 기본값을 사용하지 않으며 누락된 필수 값이 있으면 애플리케이션 시작 단계에서 실패한다.

프론트엔드는 서버에서 접근 가능한 백엔드 origin을 `BACKEND_API_URL`로 받는다. 경로, 인증정보, query를 포함하지 않은 `http` 또는 `https` origin만 허용하며 production build에서 값이 없으면 요청 처리 전에 실패한다. 외부 HTTPS 서비스에서는 `SESSION_COOKIE_SECURE=true`를 유지한다.

## 상태 점검

| 경로 | 용도 | 의존성 |
|---|---|---|
| `/livez` | 프로세스를 재시작해야 하는지 판단 | 애플리케이션 생존 상태 |
| `/readyz` | 신규 HTTP 트래픽을 받을 수 있는지 판단 | 애플리케이션 준비 상태, PostgreSQL |

Kafka는 readiness 조건에 포함하지 않는다. Kafka가 일시적으로 중단되어도 소비 이벤트와 Outbox를 PostgreSQL에 먼저 저장할 수 있고, 발행기는 복구 후 재시도하기 때문이다. Kafka 장애만으로 API 인스턴스를 제외하면 접수 가능한 요청까지 막게 된다.

운영 환경에서는 Actuator의 `health`와 `info`만 외부 노출한다. 상세 메트릭은 공개 HTTP 경로가 아닌 별도의 인증된 수집 경계에서 연결한다.
