# 직접 진행하는 무료 배포 순서

최종 점검 기준: 2026-10-18. **배포와 계정 생성은 사용자가 직접 진행합니다.** 이 작업에서 외부 계정 생성, 키 발급/폐기, 저장소 push, 외부 배포, 실제 메일 발송은 수행하지 않았습니다.

구성: Vercel Hobby(Next.js) + Render Free(Spring Boot) + Aiven Free MySQL + Brevo Free(인증/복구 메일 HTTPS API). 제공 하위 도메인으로 시작하므로 개인 도메인을 반드시 구매할 필요는 없습니다.

## 이번 점검 결과와 배포 전 필수 확인

- 로컬 계정·데이터·보안 회귀 테스트 41개 통과. 별도 메모리 DB/파일 메일로 가입→인증→로그인→재설정→탈퇴 HTTP smoke 통과.
- 프런트 lint/production build 통과. pnpm 운영 의존성 감사에서 알려진 취약점 0건, OSV Maven 149개 패키지 중 영향 패키지 0개(2026-10-18).
- 이전 단계에서 사용자가 Brevo 인증·재설정 메일 수신 및 새 비밀번호 로그인을 확인했다. 이번 점검에서는 실제 메일을 추가 발송하지 않았다.
- 화면 우선 수집·선택 우선순위·5분 공유 캐시·DB 일일 900회 예산과 최신 관측 121건을 확인했다. 전체 지역의 연속 관측 이력은 보장하지 않는다.
- **공개 전 필수**: 실제 MySQL/Docker CI 성공, 운영 HTTPS·메일 링크·지도 도메인 검증, DB 백업/복구 확인, 실제 개인정보 안내 게시.
- 개인정보 안내에는 이메일/닉네임/암호 해시/질문·답변/로그인 세션의 목적과 보존·삭제, 운영자 연락처, 호스팅·메일 제공업체와 백업 처리 방식을 실제 설정에 맞게 적는다. 운영자 정보가 없어 정책 페이지를 임의로 완성하지 않았다.
- 단일 백엔드 기준이다. CAPTCHA, MFA, 엄격한 script CSP, 미인증 가입 대기 계정 자동 정리는 아직 없다. 로그인/메일 제한은 재시작 시 초기화되며 Brevo 하루 300통 한도와 별개다. 작은 포트폴리오 공개 후 남용·용량을 관찰해야 한다.
- 인증 스모크는 file 모드에서만 실행한다. 운영에서 탈퇴 검증은 본인이 만든 별도 테스트 계정으로 수행한다.

## 0. 먼저 로컬에서 완성된 기능 확인

```powershell
# 저장소 루트: 설치 및 검증
pnpm install --frozen-lockfile
pnpm test
pnpm lint
pnpm build

# 터미널 1
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local'

# 터미널 2: 저장소 루트
pnpm dev
```

이미 3000/8080 포트에서 앱이 실행 중이면 기존 실행을 종료한 뒤 시작합니다. 데이터 폴더는 지우지 않습니다.

1. http://localhost:3000 에서 테스트 이메일을 입력하고 **인증 메일 받기**를 누릅니다. 이 단계에서는 비밀번호를 아직 설정하지 않습니다.
2. local 프로필은 실제 메일을 보내지 않고 `apps/backend/data/mail/`에 저장합니다. 아래 명령으로 입력한 이메일의 마지막 메일을 확인합니다.
3. 출력된 localhost 링크를 열어 닉네임·비밀번호·확인을 입력하고 **인증하고 가입 완료**를 누릅니다. 링크는 60분, 한 번만 사용할 수 있습니다.
4. 로그인 후 **계정 관리 → 비밀번호 재설정 메일 받기**에서 요청합니다. 같은 명령으로 새 메일을 확인합니다. 재설정 링크는 20분입니다. 변경하면 모든 기존 로그인 세션이 종료됩니다.
5. 테스트 계정으로 다시 로그인하고 **계정 관리**에서 삭제 범위를 읽은 뒤 현재 비밀번호와 `회원 탈퇴`를 입력해 탈퇴를 확인합니다.

```powershell
node scripts/read-local-mail.mjs 입력한테스트메일@example.com
# 위 전체 기능의 자동 검증: 자신이 생성한 테스트 계정·게시글만 마지막에 삭제
$env:SMOKE_FILE_MAIL_CONFIRMED="true" # file 모드임을 확인한 경우에만 설정
node scripts/smoke-auth.mjs
```

메일 링크는 비밀번호를 설정할 수 있는 일회용 비밀값입니다. 채팅·공개 이슈·스크린샷에 공유하지 마세요. local 수신함은 웹에서 제공되지 않으며 Git/Docker 이미지에서 제외됩니다.

## 1. 서울시 API 키 교체 — 직접 해야 함

대상은 **일반 인증키 / citydata_ppltn**이다. 지하철 전용 키를 신청하지 않는다.
인구 API: https://data.seoul.go.kr/dataList/OA-21778/A/1/datasetView.do
인증키 신청/관리 진입: https://data.seoul.go.kr/together/mypage/actkeyMain.do

1. 로그인 후 인증키 관리에서 현재 사용하는 일반 인증키 항목을 연다.
2. 상세/수정 화면에 재발급 기능이 있으면 안내를 읽고 재발급한다. 과거 공식 사용자 매뉴얼에는 인증키 수정 화면의 재발급 기능이 안내되어 있으나 현재 로그인 후 화면과 기존 키 무효화 시점은 직접 확인해야 한다.
3. 별도 새 인증키 신청을 허용하는 경우 새 일반 키를 신청할 수도 있다. 용도는 개인 포트폴리오의 서울시 실시간 인구 지도 서비스로 적고, 필수 URL은 화면 설명에 맞게 입력한다. 배포 전 서비스 URL이 없다면 임의 운영 주소를 꾸며 넣지 않는다.
4. 재발급 과정에서 기존 키가 즉시 무효화될 수 있다. 로컬 수집이 잠깐 중단되어도 저장 데이터는 유지된다. 기존 키가 Git에 노출되어 있으므로 폐기를 지연하지 않는다.
5. 새 키를 아래 로컬 파일에 교체하고 백엔드를 재시작해 실제 원본 응답을 확인한다. 캐시된 지도 표시만으로 새 키 성공을 판단하지 않는다.
6. 별도 발급으로 기존 키가 남아 있다면 인증키 관리의 삭제/폐기/사용중지 기능이 제공되는지 확인하여 폐기한다. 해당 기능이 없거나 재발급 후 기존 키 상태가 불명확하면 소식&참여 → 문의하기에서 기존 키 무효화를 요청한다. 공개 문의 본문에는 키를 쓰지 않는다. 재발급이 자동 폐기한다고 단정하지 않는다.
7. 완료 조건은 새 키의 원본 호출 성공과 기존 키의 무효화 확인이다. 인증키 관리 상태 또는 키를 노출하지 않는 서버 측 검증으로 확인한다.

### 2026-10-18 키 교체 검증 완료

사용자가 새 키 발급·로컬 교체 및 기존 키 폐기를 완료했다고 확인했다. 새 로컬 키는 HEAD에 남아 있는 기존 키와 다르며, 백엔드를 재시작하여 실제 원본 수집 성공을 확인했다. 기존 키의 폐기 상태를 별도 API 호출로 재검증하지는 않았다.

- 검증 거점: 가산디지털단지역(POI013), 여유, 6,000–6,500명.
- 서울시 관측 시각: 23:05 KST, 서버 확인 시각: 23:36:09 KST. 이전 21:30 관측에서 갱신되었다.
- DB 최신 관측 121건 유지, 해당 관측 이력 1건 저장, 전체 이력 중복 그룹 0건.
- 검증 호출에서 일일 사용량 77→78로 증가했으며 예산을 초기화하지 않았다. 이후 대시보드 사용에 따른 정상 수집은 계속된다.
- 대시보드에서 같은 인구 범위·관측 시각·서버 확인 시각을 확인했다. 약 31분의 원본 지연은 키 오류와 별개이며 화면에 표시된다.
- 배포, Git 이력 변경, push는 수행하지 않았다. 운영 배포 시 새 키를 Render의 SEOUL_API_KEY에도 직접 설정해야 한다.

### 로컬에 넣는 곳

`apps/backend/.env.properties`에서 기존 한 줄의 값만 바꾼다. 메일 설정 등 나머지 값은 유지한다.

```properties
seoul.api.key=새로_발급받은_키
```

따옴표 없이 입력한다. 동일 항목을 추가로 중복 작성하지 않는다. 이 파일은 local 프로필이 백엔드 작업 디렉터리에서 읽으며 Git과 Docker 컨텍스트에서 제외된다.
`application.yaml`은 `${SEOUL_API_KEY:}` 참조를 유지하고 실제 키를 다시 하드코딩하지 않는다. `.env.example`에도 넣지 않는다.
백엔드가 이미 실행 중이면 해당 프로세스를 종료한 뒤 `apps/backend` 디렉터리에서 재시작한다. 프런트 재빌드는 필요 없다.

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local --account.mail.mode=brevo'
```

이 명령은 메일 모드를 유지할 뿐 메일을 자동 발송하지 않는다. 키 교체 후 최초 원본 확인은 공통 캐시 5분/일일 예산의 영향을 받을 수 있다. 검증을 위해 예산을 초기화하지 않는다.

### Docker와 Render에 넣는 곳

현재 구성은 변경하지 않는다: Next.js는 Vercel, Spring Boot는 Docker 이미지로 Render, DB는 Aiven MySQL이다. Docker Compose 배포를 새로 추가할 필요가 없다.

- Render: 서비스 Environment의 `SEOUL_API_KEY`에 새 키 입력. `render.yaml`의 sync:false 항목이다. 나중에 배포 또는 환경변수 변경 후 재시작하여 적용한다.
- Docker: 실행 시 환경변수로 주입한다. 로컬 컨테이너 검증 시 `apps/backend/.env.example`을 참고해 Git 제외 파일 `apps/backend/.env`를 만들고 `--env-file apps/backend/.env`로 전달한다. 이 파일은 Spring이 자동으로 읽는 로컬 `.env.properties`와 다르다.
- Dockerfile의 ARG/ENV/COPY, 이미지 태그, 빌드 인자에는 키를 넣지 않는다. CA 파일은 실행 시 읽기 전용으로 `/etc/secrets/aiven-ca.pem`에 마운트하고, 운영 프로필의 HTTPS Origin·DB·메일 변수도 모두 설정한다.
- Vercel에는 서울시 키를 넣지 않는다. Vercel은 BACKEND_URL·API_PROXY_SECRET·APP_ORIGIN과 공개 카카오 지도 키만 사용한다.

저장소 루트에서의 기존 이미지 빌드 명령(키 불필요):

```powershell
docker build -t seoul-hub-api:local -f apps/backend/Dockerfile apps/backend
```

이 컴퓨터에는 Docker CLI가 확인되지 않아 위 빌드를 실행하지 않았다. GitHub Actions container 작업에서 같은 Dockerfile을 빌드한다. Dockerfile은 테스트를 제외하고 bootJar를 만들므로 backend 테스트와 mysql-migration CI도 별도로 통과해야 한다. 현재 Dockerfile/render.yaml은 아직 커밋 전이므로 원격 배포 전에 검증된 변경을 커밋·push해야 한다.

### Git에 남은 기존 키

2026-10-18 확인: 현재 Git 추적 작업 파일에서 설정된 기존 키 문자열은 발견되지 않았다. 마지막 커밋 HEAD의 `apps/backend/src/main/resources/application.yaml`에는 남아 있다. `.env.properties`·로컬 DB·repomix 출력은 Git 제외되어 있다. 이 확인은 모든 과거 브랜치/원격 복제본의 전수 감사는 아니다.

1. **먼저 기존 키를 폐기한다.** 파일 삭제나 이력 정리는 인증키 폐기가 아니다.
2. 환경변수 참조로 바뀐 application.yaml, .gitignore, 배포 파일 등 검증된 변경을 검토하여 커밋한다. 비밀 파일은 추가하지 않는다. `.gitignore`는 이미 저장된 커밋을 지우지 않는다.
3. 폐기된 키의 흔적까지 정리하려면 현재 작업 디렉터리에서 바로 이력을 변경하지 않는다. 미커밋 변경을 보존하고 협업자/브랜치/태그를 확인한 뒤 별도 새 클론에서 git-filter-repo 2.47 이상으로 **기존 키 문자열만 치환**한다. application.yaml 파일 전체 이력을 삭제할 필요는 없다.
4. 치환 파일은 저장소 밖의 비공개 파일에 폐기한 키만 넣어 만든다. 새 키를 사용하지 않는다. 명령 형태는 `git filter-repo --sensitive-data-removal --replace-text <저장소_밖_치환파일>`이다. 현재 작업 폴더에서 복사 실행하지 않는다.
5. 결과를 검토하고 영향받는 브랜치/태그의 원격 이력을 갱신해야 한다. 이는 커밋 ID 변경과 강제 push를 수반한다. 기존 클론을 그대로 merge/push하면 오염 이력이 돌아올 수 있으므로 정리 후 재클론한다. 캐시·PR·다른 사람의 fork까지 자동으로 지워지지 않는다.

이번에는 이력 재작성·강제 push·키 폐기를 실행하지 않았다. 기존 키 폐기가 실제 접근 위험을 차단하는 우선 조치이며, 이력 정리는 별도 작업이다.
공식 안내: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository

키 값은 채팅·공개 이슈·스크린샷에 보내지 않는다. 교체 완료 여부만 알려주면 로컬 적용과 실제 수집 검증을 이어갈 수 있다.

## 2. Brevo 무료 계정과 발신자 준비 — 직접 해야 함

Render 무료 서비스는 SMTP 25/465/587 포트를 차단하므로 Gmail SMTP 비밀번호 방식 대신 HTTPS API를 구현했습니다. [Render 공식 제한](https://render.com/docs/free)

1. [Brevo](https://www.brevo.com/)에서 **Free** 계정을 만들고 계정 이메일을 인증합니다. 카드 등록·유료 플랜·자동 업그레이드는 선택하지 않습니다.
2. 발신자 설정에서 본인이 소유한 이메일을 추가하고 수신한 인증 코드를 완료합니다. [공식 발신자 등록 안내](https://help.brevo.com/hc/en-us/articles/208836149-Create-a-new-sender-From-name-and-From-email)
3. Transactional email 기능이 활성화되어 있는지 확인합니다. 계정 검토/승인이 필요하다면 안내를 완료한 후 진행합니다.
4. SMTP & API 설정의 **API Keys**에서 프로젝트 전용 API 키를 발급합니다. SMTP key가 아니라 API key입니다.
5. 아래 값을 본인 비밀 저장소에 보관합니다.

| 변수 | 넣을 값 |
| --- | --- |
| `MAIL_MODE` | 운영에서는 `brevo` |
| `BREVO_API_KEY` | 발급한 API 키 |
| `MAIL_SENDER_EMAIL` | 인증 완료한 발신자 이메일 |
| `MAIL_SENDER_NAME` | `Seoul Hub` |
| `APP_PUBLIC_ORIGIN` | 나중에 확정할 Vercel HTTPS 주소, 끝 `/` 없음 |

무료 요금제는 현재 하루 300통입니다. 실제 한도는 가입 화면에서 다시 확인합니다. [Brevo 요금제](https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans)

개인 도메인 없이 Gmail 등의 주소를 쓰는 경우 Brevo가 발신 주소를 임시 `@brevosend.com` 주소로 바꿀 수 있습니다. 무료 주소는 본인이 DNS 인증을 할 수 없으므로 도달률/표시 주소에 제약이 있습니다. 무료 포트폴리오부터 확인하고, 계정 정책 때문에 발송이 거절되면 승인/발신자 설정을 해결한 뒤 진행하세요. 개인 도메인 구매나 유료 전환이 자동으로 필요한 것으로 가정하지 않습니다. [발신 도메인 변경 안내](https://help.brevo.com/hc/en-us/articles/16045394674066-Troubleshooting-Domain-authentication)

### 외부 배포 전에 본인 메일로 실제 수신 시험하기

이 단계도 사용자가 직접 실행합니다. `apps/backend/.env.properties`에 `BREVO_API_KEY`, `MAIL_SENDER_EMAIL`, `MAIL_SENDER_NAME`을 추가한 뒤 다음처럼 실행합니다. `APP_PUBLIC_ORIGIN=http://localhost:3000`은 로컬 테스트에서만 사용합니다.

```powershell
cd apps/backend
.\gradlew.bat bootRun --args='--spring.profiles.active=local --account.mail.mode=brevo'
```

본인 이메일로 회원가입/인증/재설정 메일을 요청하고 받은편지함·스팸함을 확인합니다. 성공 여부는 앱의 접수 문구만으로 판단하지 말고 실제 수신과 링크 완료까지 확인합니다. 발송 실패 시 서버에는 토큰/주소 없이 `Account email delivery failed`가 기록됩니다. 일반 로컬 파일 모드로 돌아갈 때는 마지막 `--account.mail.mode=brevo`를 빼고 재시작합니다.

**자동 smoke 스크립트는 file 모드에서만 실행합니다.** 실제 메일 모드에서는 본인 메일로 수동 검증하세요.

## 3. 배포 서비스 가입과 DB 준비 — 직접 해야 함

- [Vercel](https://vercel.com/) 개인 Hobby 계정: 비상업 포트폴리오 용도인지 확인합니다.
- [Render](https://render.com/) 계정: Web Service의 Free 등급을 선택합니다.
- [Aiven](https://aiven.io/free-mysql-database) 계정: MySQL **Free** 서비스를 생성합니다. 무료 체험 크레딧을 쓰는 유료 서비스와 구분합니다.

Aiven에서 호스트, 포트, DB 이름, 사용자, 비밀번호, CA 인증서를 저장합니다. 가능하면 앱 전용 DB 사용자를 사용하고 배포에 필요한 스키마 변경 권한을 설정합니다. 연결 수는 앱에서 최대 5개로 제한합니다. 저장소는 무료 1GB이므로 사용량을 확인합니다. [무료 MySQL 제한](https://aiven.io/docs/products/mysql/concepts/mysql-free-tier)

## 4. 저장소와 CI 확인 — 직접 해야 함

1. 변경 내역을 검토하고 본인 저장소에 commit/push합니다. `.env.properties`, `.env.local`, `data/`, 실제 인증서·비밀값은 추가하지 않습니다.
2. GitHub Actions의 테스트, 프론트 빌드, 의존성 감사, **MySQL migration**, **Docker build**가 성공하는지 확인합니다.
3. 기존 DB가 아니라 빈 Aiven DB에 처음 배포합니다. Flyway가 V1~V4를 순서대로 적용합니다. V3는 최신 관측, V4는 마지막 API 확인 시각과 호출 예산 테이블을 추가합니다.
4. 이미 V1이 적용된 DB는 V2가 이메일 인증 상태·토큰 테이블을 추가하고 기존 로그인 세션을 종료합니다. 기존 회원도 이메일 인증을 다시 해야 합니다. Flyway 이력이 없는 기존 스키마에는 자동 baseline을 켜지 말고 백업·스키마 대조·이관을 먼저 진행합니다.

이 컴퓨터에서는 Docker CLI가 없어 실제 컨테이너와 MySQL 검증은 실행하지 않았습니다. H2의 마이그레이션 검증 결과와 GitHub Actions 결과를 구분하세요.

## 5. Vercel 주소 확정 — 직접 배포

저장소를 Import하고 Root Directory `apps/frontend`, Framework `Next.js`, Node.js 22+, pnpm 11.28.5를 사용합니다. 모노레포 루트 lockfile과 workspace가 설치 범위에 포함되어야 합니다. Build Command는 `pnpm build`입니다.

Vercel 프로젝트 주소를 확정합니다. 첫 배포 시 백엔드 연결이 아직 없으면 인증 기능이 연결 오류를 표시할 수 있습니다. 이 단계의 주소를 Render의 `APP_PUBLIC_ORIGIN`에 사용하고 아래 최종 환경변수를 설정한 뒤 다시 배포합니다.

## 6. Render 백엔드 — 직접 배포

기존 `render.yaml` 또는 Docker 서비스 설정을 참고하되, 메일·Origin 항목도 포함된 템플릿의 sync:false 값을 아래 표대로 입력합니다. Dockerfile 경로는 `apps/backend/Dockerfile`, context는 `apps/backend`, health 경로는 `/actuator/health`입니다. Free 등급인지 확인합니다.

Aiven CA 인증서를 Render의 Secret File `aiven-ca.pem`으로 등록한 뒤 다음을 입력합니다.

```dotenv
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mariadb://HOST:PORT/defaultdb?sslMode=verify-full&serverSslCert=/etc/secrets/aiven-ca.pem
SPRING_DATASOURCE_USERNAME=APP_DB_USER
SPRING_DATASOURCE_PASSWORD=DB_PASSWORD
HIBERNATE_DIALECT=org.seoulhub.backend.global.config.MySqlMariaDbDriverDialect
JWT_SECRET=UNIQUE_RANDOM_32_PLUS_CHARACTERS
API_PROXY_SECRET=ANOTHER_UNIQUE_RANDOM_32_PLUS_CHARACTERS
SEOUL_API_KEY=NEW_SEOUL_KEY
SEOUL_COLLECTION_REQUEST_DELAY_MS=1000
SEOUL_COLLECTION_DAILY_BUDGET=900
MAIL_MODE=brevo
BREVO_API_KEY=YOUR_API_KEY
MAIL_SENDER_EMAIL=VERIFIED_SENDER_EMAIL
MAIL_SENDER_NAME=Seoul Hub
APP_PUBLIC_ORIGIN=https://YOUR_APP.vercel.app
```

JWT/프록시 키는 서로 다른 충분히 긴 무작위 값으로 생성합니다. Render 템플릿의 generateValue로 만든 값도 가능합니다. 직접 생성할 때는 비밀 관리 도구를 사용하고 예시 문자열을 그대로 넣지 않습니다. `API_PROXY_SECRET`만 다음 단계에서 Vercel에도 동일하게 지정합니다.

prod는 file 메일 모드, 짧거나 서로 같은 JWT/프록시 키, 수집 활성화 시 서울시 키 누락, HTTP 메일 링크 Origin, prod/local 혼용, 검증되지 않는 DB TLS 설정을 거부합니다. 비밀값을 확인하려고 로그에 출력하지 마세요.

## 7. Vercel 최종 환경변수와 카카오 지도 — 직접 설정

```dotenv
BACKEND_URL=https://YOUR_API.onrender.com
APP_ORIGIN=https://YOUR_APP.vercel.app
API_PROXY_SECRET=SAME_VALUE_AS_RENDER
NEXT_PUBLIC_KAKAO_MAP_CLIENT_KEY=KAKAO_JAVASCRIPT_KEY
```

- Render의 `APP_PUBLIC_ORIGIN`과 Vercel의 `APP_ORIGIN`은 동일한 정확한 HTTPS 주소입니다. 마지막 `/`를 넣지 않습니다.
- 서울시/메일/DB/JWT 비밀값은 Vercel 브라우저 환경변수에 넣지 않습니다. `NEXT_PUBLIC_`는 공개 지도 키에만 사용합니다.
- 변경한 환경변수를 적용해 Vercel을 재배포합니다.
- [카카오 개발자 콘솔](https://developers.kakao.com/)의 웹 플랫폼 도메인에 운영 Vercel 주소를 등록합니다. 기존 지도 앱/키가 있다면 재사용하되 허용 도메인을 제한합니다.
- Preview 주소는 운영 Origin과 달라 쓰기 요청이 차단됩니다. Preview를 쓰려면 별도의 일치하는 환경 설정이 필요하며 와일드카드 허용은 하지 않습니다.

## 8. 마지막 공개 전 검증 — 직접 수행

- 본인 이메일로 가입 → 실제 수신 → 인증/비밀번호 설정 → 로그인.
- 다른 브라우저에서도 로그인 → 재설정 → 기존 두 세션 모두 종료 → 새 비밀번호만 허용.
- 별도 테스트 계정으로 질문/답변 작성 → 탈퇴 → 로그인 불가와 콘텐츠 삭제 확인.
- 쿠키가 Secure, HttpOnly, SameSite=Lax인지 확인. 메일 링크의 토큰이 `#token=`에 있고, GET만으로 소비되지 않는지 확인.
- 공식 121개 거점의 실제 관측 시각 확인. Render 휴면에서 깨어날 때 첫 요청은 지연될 수 있으므로 잠시 후 재시도합니다.
- 백엔드 직접 `/api/v1` 요청은 프록시 비밀값 없이 403, health는 상세 없이 정상, Swagger는 운영 비활성.
- DB 백업/복구와 저장소 사용량, 메일 일일 사용량, 유료 전환 여부를 확인.

탈퇴는 활성 DB에서 계정과 관련 콘텐츠를 삭제합니다. 호스팅 백업·메일 제공업체 로그의 보존 기간은 별도이므로 실제 사용자 모집 전 개인정보 안내에 운영자 연락처·수집 목적·보존 기간·삭제/백업 처리 방침을 명시해야 합니다. 정책 문구를 확정하지 않은 채 법적 준수를 완료했다고 표시하지 마세요.

무료 Render 휴면 중에는 수집도 멈춥니다. 지속적인 수집을 보장한다고 소개하지 않고, 외부 주기 호출로 휴면을 우회하지 않습니다.

## 무료 유지·운영 확인

2026-10-18 공식 문서 재확인:
- Vercel Hobby: 비상업 개인 용도. 사용량 한도를 넘으면 기능이 제한될 수 있다. https://vercel.com/docs/plans/hobby
- Render Free: 15분 무요청 시 휴면, 복귀 약 1분, 월 750시간 공유. 로컬 파일 비영속, SMTP 25/465/587 차단이라 Brevo HTTPS API를 사용한다. 결제 수단을 추가하면 대역폭/빌드 초과 과금 조건을 별도 확인한다. https://render.com/docs/free
- Aiven Free MySQL: 카드 없이 1GB 디스크/1GB RAM, 백업 포함, 장기 무사용 시 중지 가능. 백업이 있다는 것과 복구 시험 완료는 다르다. https://aiven.io/docs/products/mysql/concepts/mysql-free-tier
- Brevo Free: 하루 300통, 미사용량 이월 없음. 추가 유료 크레딧을 구매하지 않는다. https://help.brevo.com/hc/en-us/articles/208589409-About-Brevo-s-pricing-plans

초기에는 제공 도메인만 사용한다. 유료 trial을 Free로 착각하지 않는다. 계정·플랜별 화면에서 실제 비용 0과 한도를 확인한다.

롤백: 잘못된 배포는 이전 코드로 돌아가되 적용된 DB 마이그레이션을 삭제/수정하지 않는다. 장애 시 필요하면 SEOUL_COLLECTION_ENABLED=false로 수집을 중지하고 저장된 관측을 유지한다. 비밀값 유출이면 해당 제공업체에서 폐기/교체한다. JWT 키 교체는 기존 세션 로그인을 끊을 수 있다.

## 2026-10-18 Git 정리·CI 검증 인계

### 완료한 작업

- 원본 작업 폴더의 브랜치와 미커밋 작업을 유지했다. `.local-recovery/pre-history-cleanup/`에 Git bundle, 변경 파일 사본, diff를 보관했다. 백업에는 폐기된 키의 과거 이력이 있으므로 업로드하지 않는다. 이 폴더 전체는 Git 제외다. 실행 중 DB와 새 키 파일은 원래 위치에 유지했다.
- git-filter-repo 2.47.0으로 `.local-recovery/sanitized.git` 복제본에서 폐기된 키 문자열을 치환했다. 모든 도달 가능한 blob을 검사한 결과 해당 키 잔존 0건이다. 이는 다른 비밀값 전체에 대한 감사는 아니다.
- `.local-recovery/ready-to-review`는 정리된 main 이력과 현재 미커밋 변경을 합친 검토용 폴더다. 새 키, DB, node_modules는 복사하지 않았다. 원본에서 이후 수정한 내용은 자동 동기화되지 않는다.
- 백엔드 41개 테스트 및 bootJar, 프런트 lint/production build 성공. Docker CLI 부재로 Docker 이미지와 실제 MySQL 검증은 미실행이다. GitHub CI는 아직 실행하지 않았다.
- CI에 workflow_dispatch와 실패 시 테스트 보고서 업로드를 추가했다. CI는 배포하지 않는다.

### 사용자가 진행할 순서

1. Docker Desktop을 공식 Windows 설치 안내에 따라 설치하고 Linux containers/WSL 2 엔진을 시작한다. 재부팅이 필요할 수 있다. `docker version`에서 Client와 Server가 모두 나오는지 확인한다.
   공식 안내: https://docs.docker.com/desktop/setup/install/windows-install/
2. 원래 프로젝트 루트에서 이미지 빌드를 실행한다. API 키는 빌드에 필요 없다.

```powershell
cd C:\Users\Master\seoul-hub
docker build -t seoul-hub-api:local -f apps/backend/Dockerfile apps/backend
```

3. GitHub에 연결된 별도 자동 배포가 있다면 먼저 중지한다. 이번 CI 자체에는 배포 작업이 없다. 협업자 변경·열린 PR·보호 브랜치를 확인하고, 아래 검토 폴더에서 변경 파일을 검토하여 커밋한다. `.env.properties`, `.local-recovery`, DB를 추가하지 않는다. 원래 폴더에서 커밋/push하면 옛 이력이 다시 올라갈 수 있다.

```powershell
cd C:\Users\Master\seoul-hub\.local-recovery\ready-to-review
git status --short
git add --interactive
# untracked 소스/설정/문서도 필요한 파일을 선택해 추가한 뒤 검토
git diff --cached --stat
git commit -m "Prepare secure accounts, viewport collection and deployment CI"
git remote set-url origin https://github.com/suwon98/seoul-hub.git
```

4. 2026-10-18 원격 main은 `77f6684d37ece5d184a459306a3c9e0ad9417a77`이었다. 아래 명령은 그 이후 main이 변경되었으면 거부한다. 거부되면 무조건 --force로 바꾸지 말고 변경을 다시 확인한다. 이번 정리에서는 main만 갱신하며 --mirror로 로컬 내부 refs를 원격에 보내지 않는다. 다른 원격 브랜치·태그가 추가되었다면 별도 확인이 필요하다.

```powershell
git push --force-with-lease=refs/heads/main:77f6684d37ece5d184a459306a3c9e0ad9417a77 origin main:main
```

5. GitHub Actions → Seoul Hub CI에서 backend, frontend, mysql-migration, container 네 작업이 모두 성공했는지 확인한다. 실패하면 로그를 확인해 수정한다. 수동 재실행도 가능하다. Docker를 설치하지 않는 경우 container와 mysql-migration CI를 통해 검증할 수 있지만, 성공 전까지 빌드 완료로 간주하지 않는다.
6. 원격 반영 뒤에는 새 클론을 작업 기준으로 삼고, 기존 폴더에서는 push하지 않는다. 필요한 로컬 비밀 설정과 DB는 앱을 종료한 뒤 별도로 안전하게 옮긴다. 옛 백업은 원격에 올리지 않는다. PR·fork·캐시는 강제 push만으로 모두 제거되지 않는다.
   공식 안내: https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository

현재 원격 push, 원본 이력 변경, 배포는 수행하지 않았다. 다음 확인 지점은 Docker 엔진 실행 또는 사용자의 검토·커밋·원격 반영 후 CI 결과다.

## 2026-10-19 Docker·실제 MySQL 로컬 검증 완료

- Docker Desktop Linux 엔진 정상화 후 `seoul-hub-api:local` 최종 이미지 빌드 성공.
- MySQL 8.4.11 임시 컨테이너로 MigrationTests 통과. 빈 DB에 최종 이미지를 연결해 V1~V4 성공, Hibernate validate 및 HTTP health UP 확인.
- 동일 DB로 앱 재시작 후 마이그레이션 4건 유지, up to date, health UP 확인. 실행 사용자 uid=999(app), root 아님.
- 수정 후 전체 백엔드 회귀 테스트 41개 성공.
- CI 임시 MySQL 인증에 allowPublicKeyRetrieval=true 추가. 격리된 테스트 전용이며 운영 URL은 sslMode=verify-full을 유지한다.
- MariaDB Connector/J와 MySQLDialect 조합의 메타데이터 조회에서 DB 이름이 대문자로 변환되는 문제를 발견했다. MySqlMariaDbDriverDialect는 MySQL SQL 동작을 유지하고 Hibernate MariaDBDialect와 같은 대소문자 보존 fallback을 적용한다. Render, 환경변수 예시, CI, 배포 문서에 반영했다.
- 이번 컨테이너 실행은 외부 API·메일을 비활성화한 로컬 검증 설정이다. Aiven TLS/운영 prod 환경 및 GitHub Actions 실제 실행 성공까지 의미하지 않는다.
- 임시 앱·MySQL 컨테이너와 테스트 볼륨·네트워크는 제거했다. 빌드 이미지는 남겨 두었다. 기존 로컬 DB·비밀 설정·작업 이력은 보존했고 배포·push는 하지 않았다.
- 이번 수정 파일은 정리된 검토 폴더 `.local-recovery/ready-to-review`에도 동기화했다. 원격 반영 전 이 폴더에서 검토·커밋한다.
