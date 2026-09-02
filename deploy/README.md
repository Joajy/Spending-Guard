# 운영 배포

Linux 서버 한 대에서 공개 진입점과 애플리케이션, 데이터 서비스를 분리해 실행하는 구성이다. 외부에는 Caddy의 80·443 포트만 열고 프론트엔드, 백엔드, PostgreSQL과 Kafka 포트는 공개하지 않는다.

## 준비 사항

- Docker Engine과 Docker Compose plugin
- 서버를 가리키는 도메인의 A 또는 AAAA 레코드
- 외부에서 접근 가능한 80·443 TCP 포트와 443 UDP 포트
- 운영 SMTP 계정
- 발행이 완료된 동일 버전의 백엔드·프론트엔드 GHCR 이미지

`deploy/.env.example`을 `deploy/.env`로 복사한 뒤 실제 값을 입력하고 파일 접근 권한을 운영 계정으로 제한한다. `.env`는 Git에 추가하지 않는다.

```bash
docker compose --env-file deploy/.env -f deploy/compose.production.yml config --quiet
docker compose --env-file deploy/.env -f deploy/compose.production.yml pull
docker compose --env-file deploy/.env -f deploy/compose.production.yml up -d --wait
```

Caddy는 도메인의 TLS 인증서를 자동으로 발급하고 갱신한다. 애플리케이션은 `/readyz`가 성공한 뒤에만 다음 서비스의 시작 조건을 만족한다.

롤백은 `.env`의 `RELEASE_VERSION`을 이전 정상 버전으로 바꾸고 `pull`, `up -d --wait`를 다시 실행한다. 데이터베이스 migration이 하위 호환되지 않는 릴리스는 이미지 롤백만으로 복구할 수 없으므로 배포 전에 migration 복구 절차를 별도로 준비한다.

현재 구성은 단일 서버용이다. 서버 방화벽, 로그 수집과 가용성 이중화는 호스팅 환경을 정한 뒤 그 환경의 관리형 기능과 연결한다.

PostgreSQL 백업 파일 생성과 복구 훈련 방법은 [`docs/deployment/database-backup-recovery.md`](../docs/deployment/database-backup-recovery.md)를 따른다. 백업 파일은 저장소에 포함하지 않고 암호화된 별도 저장소로 복사한다.

운영 지표와 경보는 Prometheus와 Grafana로 확인한다. Grafana는 서버의 loopback 주소에만 열리며 접근 방법과 경보 기준은 [`docs/deployment/operational-observability.md`](../docs/deployment/operational-observability.md)에 정리했다.
