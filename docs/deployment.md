# 배포 구성 참고

현재 운영 상태와 변경 순서는 [운영 안내](manual-launch-guide.md)를 따른다.

## 준비된 파일

- `apps/backend/Dockerfile`: Java 21 멀티스테이지 빌드, 비루트 실행, 메모리 제한을 고려한 JVM 설정.
- `apps/backend/.dockerignore`: 소스/빌드 설정만 허용해 로컬 키와 DB를 이미지에서 제외.
- `render.yaml`: 백엔드 배포 템플릿. DB와 서울시 키는 별도 입력.
- `application-prod.yaml`: Flyway 활성화, Hibernate validate, Swagger 비활성화.
- `.github/workflows/ci.yml`: 테스트·프론트 빌드·의존성 감사·MySQL 마이그레이션·Docker 빌드.
- `.github/dependabot.yml`: 의존성 갱신 제안.

## 실제 배포 순서

1. Git 이력에 노출된 서울시 키를 재발급하고 기존 키를 폐기합니다. 새 키는 환경변수에만 저장합니다. 공개 저장소에서 이력을 정리하더라도 키 폐기는 별도로 필요합니다.
2. Aiven에서 MySQL을 만들고 접속 주소·사용자·암호·CA 인증서를 확보합니다. 빈 데이터베이스에 배포합니다. 기존 데이터가 있다면 백업 후 V1 스키마와 수동 비교/이관해야 하며 자동 baseline은 켜지 않습니다.
3. Render에서 저장소의 Blueprint 또는 Docker 서비스로 배포합니다. Aiven CA를 Secret File `aiven-ca.pem`으로 등록합니다. 아래 값을 입력합니다.
4. Vercel에서 같은 저장소를 연결하고 Root Directory를 `apps/frontend`로 설정합니다. 모노레포의 루트 lockfile/워크스페이스를 포함하도록 설치 범위를 유지하고 pnpm 11.28.5를 사용합니다. 프레임워크는 Next.js, 빌드는 `pnpm build`입니다. 첫 배포 주소를 확인한 후 `APP_ORIGIN`을 정확히 지정해 다시 배포합니다.
5. 카카오 콘솔의 웹 플랫폼 도메인에 Vercel HTTPS 주소를 추가합니다. Preview 배포는 고정 운영 Origin과 다르므로 쓰기 요청이 차단되는 것이 정상입니다. Preview가 필요하면 해당 환경의 정확한 Origin을 따로 지정하고 와일드카드를 사용하지 않습니다.
6. 아래 운영 검증을 완료한 뒤 공개 링크를 포트폴리오에 넣습니다.

### Render 환경변수

```dotenv
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:mariadb://HOST:PORT/defaultdb?sslMode=verify-full&serverSslCert=/etc/secrets/aiven-ca.pem
SPRING_DATASOURCE_USERNAME=APP_DB_USER
SPRING_DATASOURCE_PASSWORD=SECRET
HIBERNATE_DIALECT=org.seoulhub.backend.global.config.MySqlMariaDbDriverDialect
JWT_SECRET=UNIQUE_RANDOM_SECRET_AT_LEAST_32_CHARACTERS
API_PROXY_SECRET=ANOTHER_UNIQUE_RANDOM_SECRET_AT_LEAST_32_CHARACTERS
SEOUL_API_KEY=NEWLY_ISSUED_KEY
SEOUL_COLLECTION_REQUEST_DELAY_MS=1000
SEOUL_COLLECTION_DAILY_BUDGET=900
APP_PUBLIC_ORIGIN=https://YOUR_APP.vercel.app
MAIL_MODE=brevo
BREVO_API_KEY=YOUR_API_KEY
MAIL_SENDER_EMAIL=VERIFIED_SENDER_EMAIL
MAIL_SENDER_NAME=Seoul Hub
```

템플릿은 두 비밀값을 자동 생성하도록 되어 있습니다. `API_PROXY_SECRET`만 Vercel에도 동일하게 설정합니다. JWT 비밀값은 Render에만 둡니다. JDBC 드라이버는 MySQL과 호환되는 MariaDB Connector/J이며 CA 검증과 호스트 검증을 모두 켭니다. [공식 드라이버 TLS 설정](https://mariadb.com/docs/connectors/mariadb-connector-j/using-tls-ssl-with-mariadb-java-connector)

### Vercel 환경변수

```dotenv
BACKEND_URL=https://YOUR_API.onrender.com
APP_ORIGIN=https://YOUR_APP.vercel.app
API_PROXY_SECRET=SAME_VALUE_AS_RENDER
NEXT_PUBLIC_KAKAO_MAP_CLIENT_KEY=DOMAIN_RESTRICTED_BROWSER_KEY
```

프론트는 `/api/v1`로 요청하고 Next.js 서버가 백엔드로 전달합니다. 서울시/DB/JWT 키는 프론트 프로젝트에 넣지 않습니다. 브라우저에 공개되는 지도 키만 `NEXT_PUBLIC_`를 사용합니다. HTTPS Origin에서 인증 쿠키에는 Secure가 적용됩니다.

## 운영 검증과 복구

- GitHub Actions의 테스트, MySQL migration, Docker build가 모두 성공하는지 확인합니다. 실제 MySQL 마이그레이션과 Docker 실행 검증도 완료했습니다.
- `/actuator/health`는 상태만 공개하며 운영 Swagger는 접근 불가여야 합니다.
- 실제 서비스의 회원가입·로그인·Q&A·로그아웃과 쿠키 Secure/HttpOnly/SameSite를 확인합니다. `smoke-auth.mjs`는 로컬 데이터 생성용이라 외부 주소에 실행하지 않습니다.
- 백엔드 API 직접 호출은 내부 프록시 비밀값 없이 403이어야 합니다. 외부 Origin의 브라우저 쓰기도 403이어야 합니다.
- 서울시 공식 121개 거점의 원본 관측 시각·인구·중복 방지를 확인합니다. 서버 재배포 후 계정·게시글·통계가 남는지도 확인합니다.
- 서버 휴면 후 첫 요청의 지연/재시도를 확인합니다. 프록시는 50초 후 503 안내를 반환할 수 있습니다.
- DB 백업/내보내기와 복구를 실제로 시험하고 1GB 저장소 사용량을 확인합니다. 배포 전후 오류율, 메모리, DB 연결 수를 점검합니다.
- 코드 롤백은 직전 커밋 이미지로 합니다. DB 변경은 되돌릴 수 있다고 가정하지 말고 호환 가능한 후속 마이그레이션 또는 검증된 백업 복구를 사용합니다.

현재 구성은 백엔드 1개 인스턴스를 전제로 합니다. 여러 인스턴스로 늘릴 때 예약 수집의 분산 잠금과 공유 요청 제한 저장소가 필요합니다.
