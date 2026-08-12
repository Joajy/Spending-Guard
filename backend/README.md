# Backend

## 개발 환경

- Java 21
- Spring Boot 3.5.16
- Gradle 8.14.3 Wrapper
- PostgreSQL 16
- Apache Kafka 3.8.1

## IntelliJ에서 실행하기

1. 저장소를 clone한 뒤 IntelliJ에서 `backend/build.gradle`을 Gradle 프로젝트로 엽니다.
2. Project SDK와 Gradle JVM을 Java 21로 설정합니다.
3. 저장소 루트에서 `docker compose up -d`를 실행합니다.
4. `SpendingGuardApplication`을 실행합니다.
5. `GET http://localhost:8080/api/v1/status`가 정상 응답하는지 확인합니다.

소비 알림은 다음 API로 접수할 수 있습니다.

```http
POST http://localhost:8080/api/v1/spend-events
Content-Type: application/json

{
  "source": "MANUAL_TEXT",
  "message": "테스트상점 12,800원 결제",
  "occurredAt": "2026-08-13T01:00:00Z"
}
```

Postman에서는 저장소의 `postman/Spending-Guard.postman_collection.json`을 불러오면 상태 확인, 정상 접수, 중복 접수를 순서대로 확인할 수 있습니다.

로컬 기본값은 `application.yml`에 정의되어 있습니다. 실제 비밀번호나 외부 서비스 키는 파일에 저장하지 않고 환경 변수로 주입합니다.

## 패키지 구조

기능을 먼저 나누고, 기능 안에서는 책임별로 분리합니다.

```text
com.joajy.spendingguard
├── spendevent
│   ├── api           # HTTP 요청·응답과 예외 처리
│   ├── application   # 소비 이벤트 접수 흐름
│   ├── domain        # 중복 키, 개인정보 제거, 상태 규칙
│   └── persistence   # 소비 원문 엔티티와 저장소
├── outbox             # 후속 메시지 발행을 위한 Outbox
└── support            # 상태 확인, 시간 등 공통 지원 기능
```

API DTO는 `api` 안에서 application Command와 Receipt로 변환합니다. 따라서 HTTP 표현이 바뀌더라도 접수 로직과 저장 모델에 변경이 번지지 않습니다.

## 테스트

Windows:

```powershell
.\gradlew.bat clean test jacocoTestReport
```

macOS 또는 Linux:

```bash
./gradlew clean test jacocoTestReport
```

테스트 결과는 `build/reports/tests/test/index.html`, 커버리지는 `build/reports/jacoco/test/html/index.html`에서 확인할 수 있습니다.
