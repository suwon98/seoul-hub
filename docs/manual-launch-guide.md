# 배포 및 운영 안내

Vercel의 Next.js 프런트엔드, Render의 Docker 백엔드, Aiven MySQL, Brevo HTTPS 메일 API로 운영한다. 공개 주소: https://seoul-hub.vercel.app

## 최신 완료 상태 — 2026-10-19

기능 개발과 운영 핵심 흐름 검증을 완료했다. 운영 반영 커밋은 `21af5dc65496625e3027fcb1e105790c5a58b9f4`이며, 배포와 push는 사용자가 직접 수행했다. 아래 이전 날짜/단계의 기록은 당시 상태이며 이 요약을 최신 기준으로 사용한다.

- GitHub Seoul Hub CI의 backend, frontend, mysql-migration, container 네 작업 성공 확인: https://github.com/suwon98/seoul-hub/actions/runs/39242118157
- 로컬 백엔드 43개 테스트, 프런트 lint/build 통과. 실제 MySQL 마이그레이션 및 Docker 실행도 앞 단계에서 검증했다.
- 운영 HTTP 확인: 홈에서 dashboard로 307 이동, dashboard/login/signup 200, 거점 API 121건 반환, 비로그인 계정 조회 401.
- 사용자 운영 확인: 이메일 인증·가입·로그인, 비밀번호 재설정, 이전 비밀번호 및 기존 세션 차단, 로그아웃, 탈퇴 및 다른 창 세션/탈퇴 계정 로그인 차단.
- 최신 사용자 운영 확인: 일반 창과 시크릿 창에서 번갈아 같은 계정으로 새 로그인 시 이전 창 세션 차단. 대시보드 첫 화면과 로그인·회원가입 동선 정상.
- 지도 실제 인구 표시와 커뮤니티/계정관리에서 대시보드로 돌아올 때 지도 복원도 사용자 확인 완료.
- 자동 브라우저 렌더링 검증은 도구 오류로 수행하지 못했으며 위 화면 동작은 사용자 확인 결과다. 보안 전수 감사나 장애 복구 시험 완료를 의미하지 않는다.

### 운영 후 남은 확인

- 실제 사용자 모집 전 운영자 연락처와 실제 보존/삭제·제공업체 처리 방식을 반영한 개인정보 안내를 확정하고 게시한다. 운영자 정보가 없어 임의 작성하지 않았다.
- DB 백업 복구를 운영 DB가 아닌 별도 시험 DB에서 검증한다. 현재 백업 복구 시험은 미완료다.
- 모바일에서 지도 높이, 검색/필터, 가입 폼, 계정관리 화면을 확인한다. 현재 모바일 검증 결과는 제공되지 않았다.
- 서비스별 사용량/청구 화면, DB 저장량, 메일 발송량 및 수집 예산을 주기적으로 확인한다. 서버 휴면 시 지연될 수 있고 상시 수집을 보장하지 않는다.


## 변경 반영 절차

환경변수 자리표시자와 Docker 설정은 [배포 구성](deployment.md)을 따른다. 실제 키·비밀번호·인증서 내용은 문서에 입력하지 않는다.

1. 정리된 저장소에서 변경 내용과 비밀값 포함 여부를 검토하고 로컬 검증 후 커밋한다. push와 배포는 운영자가 직접 수행한다.
2. GitHub Actions의 backend, frontend, mysql-migration, container 네 작업 성공을 확인한다.
3. Render Dockerfile은 `apps/backend/Dockerfile`, context는 `apps/backend`, health는 `/actuator/health`다. 백엔드 변경을 해당 커밋으로 배포한다.
4. Aiven CA는 Secret File `aiven-ca.pem`으로 등록한다. 컨테이너 경로는 `/etc/secrets/aiven-ca.pem`이며 PC 경로가 아니다. 앱 전용 DB 사용자와 TLS 검증을 사용한다.
5. Vercel Root Directory는 `apps/frontend`, Node.js 22, pnpm 11.28.5이며 루트 lockfile을 사용한다. 프런트 변경을 해당 커밋으로 배포한다.
6. 환경변수는 Save Only 후 새 배포로 적용한다. 일반 재배포는 기존 빌드 캐시를 사용할 수 있다. 캐시가 원인으로 의심되는 빌드 문제일 때만 캐시 없이 재시도한다.
7. Render `APP_PUBLIC_ORIGIN`과 Vercel `APP_ORIGIN`은 동일한 정확한 HTTPS origin으로 지정한다. 끝 슬래시는 넣지 않는다. 카카오 웹 허용 도메인에도 등록한다. Preview는 별도의 origin과 환경 설정이 필요하다.
8. health, 지도 관측 시각, 로그인과 이메일 링크를 확인한다. 두 창에서 번갈아 로그인해 이전 창의 인증 요청이 거부되는지 확인한다.

## 데이터와 장애 대응

적용된 Flyway 마이그레이션은 수정·삭제하지 않는다. 기존 운영 DB를 초기화하거나 자동 baseline하지 않는다. 변경 전 백업과 호환성을 확인한다.

서버 휴면 중에는 수집이 멈춘다. 마지막 관측 시각과 지연 상태를 표시하며 누락된 시간을 실제 관측처럼 채우지 않는다. 프록시 요청이 50초를 넘으면 503 안내가 나올 수 있다. 수집 정책은 [인구 데이터 안내](population-data.md)를 따른다.

장애 시 이전 정상 코드로 롤백하되 DB 호환성을 먼저 확인한다. 수집만 중단할 때는 `SEOUL_COLLECTION_ENABLED=false`로 설정하고 저장된 관측을 유지한다. 비밀값이 노출되면 제공업체에서 폐기·교체하고 환경변수를 갱신한다. 이력 정리는 키 폐기를 대신하지 않는다.

운영 DB에 로컬 데이터 생성용 `smoke-auth.mjs`를 실행하지 않는다. 로컬 smoke는 file 메일 모드 확인 후 실행한다. 실제 메일 시험은 본인 테스트 계정으로 수신과 링크 완료까지 확인한다.

## 후속 작업 경로와 주의사항

이 컴퓨터의 작업 기준은 `C:/Users/Master/seoul-hub/.local-recovery/ready-to-review`다. 원본 `C:/Users/Master/seoul-hub`에는 옛 Git 이력과 로컬 설정·DB가 남아 있으므로 원본에서 commit/push하거나 변경을 통째로 덮어쓰지 않는다.

- 작업 시작 전 `git status --short`, `git branch --show-current`, `git log -1 --oneline`으로 위치와 변경을 확인한다.
- `.env.properties`, `.env.local`, DB 파일, 실제 인증서, `.local-recovery` 백업은 커밋하지 않는다. 키는 채팅·로그·스크린샷에도 포함하지 않는다.
- 과거 이력 정리를 위한 force push는 완료됐다. 이후에는 일반 push를 사용하며 `--force`나 `--mirror`를 사용하지 않는다.
- 다른 PC에서는 원격 저장소를 새로 clone한다. 필요한 로컬 비밀 설정은 Git 외부에서 별도로 전달한다.
- 기존 미커밋 변경은 임의로 reset/clean하지 않고 확인 후 보존한다.
