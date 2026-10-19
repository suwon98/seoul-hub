# Seoul Hub

서울 주요 지역의 인구 혼잡도와 시간별 추이를 확인하고, 회원끼리 질문과 답변을 나누는 포트폴리오 서비스입니다. 서울시 실제 관측값을 사용하며 데이터가 지연되면 마지막 관측 시각과 지연 상태를 표시합니다.

## 구조

```text
브라우저 → Next.js 화면 / 같은 출처 API 프록시 → Spring Boot → MySQL
                                            └→ 서울시 열린데이터광장
```

- 프론트엔드: Next.js 16.4 / React 19 / TypeScript / Tailwind / Recharts / 카카오 지도.
- 백엔드: Java 21 / Spring Boot 4.0 / Spring Security / JPA / Flyway.
- 서울시 공식 121개 거점을 지도에 표시합니다. 하루 900회 앱 예산을 유지하며 검색·선택 거점, 확대된 지도 안 최대 12곳 순으로 갱신합니다. 서버 확인 결과는 5분간 공유하고 전체 순환 수집은 하지 않습니다.
- 서울시 지역명 별칭을 변환하고 원본 관측 시각을 보존합니다. 같은 지역·시각은 중복 저장하지 않습니다.
- 혼잡도는 관측 당시 등급을 유지하며, 관측 나이를 별도로 표시합니다. 45분 이상은 지연, 2시간 이상은 오래된 관측으로 구분합니다.
- 최신 관측은 별도 테이블에 유지하고, 원시 관측은 48시간, 시간별 집계는 30일 보관합니다. 지연 수신을 반영하도록 최근 24개 완료 시간을 5분마다 다시 집계합니다. 차트에는 최근 24개 시간별 집계를 표시하며 집계 전에는 최근 원시 관측을 사용합니다.
- 이메일 인증 후 닉네임·비밀번호 설정, 비밀번호 재설정, 현재 비밀번호 확인 후 회원 탈퇴, 회원 전용 Q&A. HttpOnly 쿠키와 DB 로그인 세션을 사용하고 로그아웃 시 재사용을 차단합니다. 같은 계정으로 새로 로그인하면 기존 세션은 만료됩니다. 첫 화면은 공개 대시보드이며 상단에서 로그인·회원가입으로 이동합니다.

## 로컬 실행

Node.js 22+, pnpm 11.28.5, JDK 21이 필요합니다.

```powershell
pnpm install --frozen-lockfile
# 터미널 1
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
# 터미널 2: 저장소 루트
pnpm dev
```

로컬 DB는 `apps/backend/data/`의 H2 파일입니다. 서울시 키는 환경변수 `SEOUL_API_KEY` 또는 Git에서 제외한 `apps/backend/.env.properties`의 `SEOUL_API_KEY=...`로 지정합니다. 이 파일은 백엔드 디렉터리에서 실행하는 local 프로필에서만 읽습니다. `JWT_SECRET`도 같은 방식으로 지정할 수 있습니다. 키가 없으면 외부 수집을 생략합니다.

`apps/frontend/.env.example`을 참고해 `.env.local`을 설정합니다. 카카오 JavaScript 키는 브라우저 공개용이며 카카오 콘솔의 허용 웹 도메인 제한이 필요합니다. 서버용 비밀값에 `NEXT_PUBLIC_` 접두사를 붙이지 마세요.

- 대시보드: http://localhost:3000/dashboard
- 회원가입: http://localhost:3000/signup / 로그인: http://localhost:3000/login
- 로컬 API 명세: http://localhost:8080/swagger-ui/index.html

## 검증

```powershell
pnpm lint
pnpm build
pnpm test
# 프론트·백엔드 실행 후 로컬 HTTP 인증 통합 검증 (검증용 계정/게시글 생성)
$env:SMOKE_FILE_MAIL_CONFIRMED="true" # 먼저 백엔드 file 메일 모드 확인
node scripts/smoke-auth.mjs
# Java 런타임 공개 취약점 조회 (패키지 이름/버전만 OSV로 전송)
cd apps/backend
.\gradlew.bat dependencyInventory
cd ../..
python scripts/audit-java.py
pnpm audit --prod
```

CI에는 백엔드 테스트, 프론트 lint/build/audit, 실제 MySQL 8.4 마이그레이션 검증, Docker 빌드를 구성했습니다. 로컬 H2 검증과 실제 배포 DB 검증은 구분합니다. 빌드 폴더가 잠긴 경우 `NEXT_BUILD_DIR=.next-verify`를 build/start 모두에 지정할 수 있습니다.

## 이메일 인증·계정 복구·탈퇴

회원가입은 이메일만 입력합니다. 메일의 일회용 링크를 열고 닉네임·비밀번호를 설정해야 로그인할 수 있습니다. 기존 계정도 이메일 인증이 필요합니다. 인증 링크는 60분, 재설정 링크는 20분이며 재발송하면 이전 링크는 무효화됩니다. 비밀번호 재설정은 모든 기기의 로그인 세션을 종료합니다.

local 프로필은 외부 발송 없이 `apps/backend/data/mail/`에 메일을 저장합니다.

```powershell
node scripts/read-local-mail.mjs your-test@example.com
$env:SMOKE_FILE_MAIL_CONFIRMED="true" # 먼저 백엔드 file 메일 모드 확인
node scripts/smoke-auth.mjs
```

탈퇴는 로그인한 계정의 현재 비밀번호와 `회원 탈퇴` 확인 문구가 필요합니다. 계정·본인 질문/답변·본인 질문에 달린 답변을 활성 DB에서 삭제합니다. 다른 사람의 질문은 유지합니다. 메일/호스팅 서비스의 로그와 백업 보존은 별도 운영 정책으로 관리합니다.

운영 메일은 Brevo HTTPS API로 구현했습니다. 실제 메일 서비스 가입·발신자 인증·키 설정·수신 검증은 사용자가 직접 진행합니다. 외부 배포도 사용자가 직접 수행합니다.

## 배포와 보안

**Vercel + Render + Aiven MySQL**로 운영 중입니다. [서비스 바로가기](https://seoul-hub.vercel.app/). 서버 휴면 중에는 수집이 중단되므로 관측 시각과 지연 상태를 함께 표시합니다.

- **[배포 및 운영 안내](docs/manual-launch-guide.md)** — 완료 상태, 후속 작업 경로, 변경 반영과 복구 절차
- [배포 구성 참고](docs/deployment.md)
- [보안 변경·검증 결과·남은 항목](docs/security-review.md)

운영은 `prod` 프로필, 외부 MySQL, 검증된 TLS 연결, 무작위 JWT/프록시 비밀값을 요구합니다. Flyway로 스키마를 생성하고 Hibernate는 검증만 합니다. 키 교체·기존 키 폐기와 Git 이력 정리를 완료했습니다. 실제 비밀값은 저장소에 넣지 않습니다.

전체 거점 출처·저장 구조·수집 설정: [인구 데이터 가이드](docs/population-data.md).
