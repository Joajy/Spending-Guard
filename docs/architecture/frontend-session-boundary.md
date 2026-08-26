# 프론트엔드 세션 경계

프론트엔드는 로그인 응답의 Access Token과 Refresh Token을 브라우저 JavaScript에 전달하지 않는다. Next.js Route Handler가 백엔드 인증 API를 호출하고 토큰을 `HttpOnly`, `SameSite=Lax` 쿠키에 저장한다. 대시보드 요청도 Route Handler를 통과하므로 화면 코드는 토큰 값에 접근하지 않는다.

```text
Browser → Next.js BFF → Spring Boot API
             └─ HttpOnly session cookies
```

대시보드 조회가 `401 Unauthorized`를 받으면 BFF가 Refresh Token으로 토큰을 한 번 회전하고 원래 요청을 재시도한다. 갱신이 거부되면 세션 쿠키를 지우고 로그인 화면으로 이동시킨다. 로그아웃은 백엔드 토큰 폐기를 먼저 요청하되, 백엔드가 일시적으로 응답하지 않더라도 로컬 쿠키는 제거한다.

이 경계는 토큰 탈취 가능성을 줄이고 인증 갱신과 오류 처리를 화면 컴포넌트에서 분리하기 위해 둔다. 현재 쿠키는 동일 사이트 웹 클라이언트를 전제로 하며, 외부 도메인 배포 시에는 CSRF 정책과 쿠키 도메인을 배포 환경에 맞춰 추가 검토한다.
