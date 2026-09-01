FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S spendingguard && adduser -S -G spendingguard spendingguard
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar
USER spendingguard
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
