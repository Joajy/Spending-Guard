# 컨테이너 이미지 릴리스

배포 환경은 같은 소스와 Dockerfile로 만들어진 버전 고정 이미지를 사용한다. Pull Request에서는 백엔드와 프론트엔드 이미지를 실제로 빌드하지만 registry에는 올리지 않는다.

## 이미지 이름

- `ghcr.io/joajy/spending-guard-backend`
- `ghcr.io/joajy/spending-guard-frontend`

`main`에 포함된 커밋에 `vMAJOR.MINOR.PATCH` 형식의 태그가 생성되면 두 이미지를 GitHub Container Registry에 발행한다. 예를 들어 `v1.2.3`은 `1.2.3`과 `1.2` 태그를 만든다. `latest` 태그는 자동으로 변경하지 않는다.

릴리스 workflow는 다음 조건을 확인한다.

- 태그가 정확한 SemVer 형식인지 확인한다.
- 태그가 가리키는 커밋이 `main` 이력에 포함됐는지 확인한다.
- PR 검증에는 registry 쓰기 권한을 부여하지 않는다.
- 발행 단계에만 GitHub의 단기 `GITHUB_TOKEN`과 `packages: write` 권한을 사용한다.
- workflow가 실행하는 외부 Action은 검증한 커밋 SHA로 고정한다.
- 이미지는 일반 사용자 권한으로 실행하며 SBOM과 provenance를 함께 발행한다.

최초 발행 후 외부 배포 플랫폼에서 이미지를 받아야 한다면 GitHub Packages 화면에서 이미지 공개 범위를 확인한다. 릴리스 태그 생성과 이미지 공개 범위 변경은 배포 승인 이후에 수행한다.
