# 최초 릴리스 체크리스트

`0.1.0`은 로컬 전체 사용자 여정과 운영 배포 계약이 검증된 첫 번째 버전이다. 태그를 만들면 백엔드와 프론트엔드 이미지가 GHCR에 실제 발행되므로 아래 준비가 끝난 뒤 별도 승인을 받아 진행한다.

## 태그 생성 전

- `main`의 Backend CI, Frontend CI, Full-stack smoke와 Deployment config가 모두 성공했는지 확인한다.
- `python scripts/release/verify_release_readiness.py --tag v0.1.0`이 통과하는지 확인한다.
- `CHANGELOG.md`의 기능과 제한 사항이 실제 구현 범위와 일치하는지 검토한다.
- 운영 도메인의 DNS와 서버의 80·443 포트가 준비됐는지 확인한다.
- 데이터베이스, JWT, SMTP와 Grafana 비밀값을 운영 비밀 저장소에 등록한다.
- PostgreSQL 백업 파일을 복사할 별도 암호화 저장소와 복구 담당자를 정한다.
- GHCR 이미지 공개 범위 또는 서버가 사용할 package 읽기 권한을 확인한다.

## 승인 후 태그 생성

다음 명령은 두 컨테이너 이미지를 실제 발행하므로 배포 승인을 받은 뒤 실행한다.

```bash
git switch main
git pull --ff-only origin main
git tag -a v0.1.0 -m "Spending Guard 0.1.0"
git push origin v0.1.0
```

Container images workflow에서 다음 결과를 확인한다.

- 태그가 `main` 커밋을 가리킨다.
- 백엔드와 프론트엔드 publish job이 모두 성공한다.
- 각 이미지에 `0.1.0`, `0.1` 태그와 SBOM, provenance가 생성된다.
- 실패한 job이나 의도하지 않은 `latest` 태그가 없다.

## 서버 반영

`deploy/.env`의 `RELEASE_VERSION`을 `0.1.0`으로 설정하고 운영 Compose의 `pull`, `up -d --wait`를 실행한다. 배포 직후 다음을 확인한다.

- `/livez`, `/readyz`와 로그인 화면이 정상이다.
- 회원가입, 이메일 인증, 로그인과 소비 등록 사용자 여정을 한 번 수행한다.
- Grafana에서 backend up, HTTP 요청량과 JVM 지표가 수집된다.
- Prometheus의 다섯 개 경보 규칙이 오류 없이 로드된다.
- 배포 직전 PostgreSQL 백업과 SHA-256 파일이 별도 저장소에 존재한다.

## 중단과 롤백

이미지 발행에 실패하면 성공한 한쪽만 배포하지 않는다. 서버 반영 이후 문제가 발생하면 `RELEASE_VERSION`을 이전 정상 버전으로 바꾸고 다시 기동한다. 데이터베이스 migration이 하위 호환되지 않으면 이미지 롤백만 수행하지 않고 검증한 백업으로 복구한다.
