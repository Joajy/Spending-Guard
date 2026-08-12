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

로컬 기본값은 `application.yml`에 정의되어 있습니다. 실제 비밀번호나 외부 서비스 키는 파일에 저장하지 않고 환경 변수로 주입합니다.

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
