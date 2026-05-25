# 아키텍처 결정 로그 (Architecture Decision Log)

## 1. 리포지토리 구조 및 패키지 매니저
- **결정:** `pnpm Workspaces` 기반 모노레포(Monorepo) 구성
- **이유:** 단일 저장소 내에서 프론트/백엔드를 관리하여 실시간 소켓 통신 타입 공유 극대화. pnpm을 통해 무료 CI/CD 배포 플랫폼의 디스크 자원 및 빌드 시간 절약.

## 2. 개발 환경 분리 및 IDE 이원화
- **결정:** 백엔드는 IntelliJ (Java 21 + Spring Boot 4.0.6), 프론트엔드는 VS Code (Next.js 16) 채택
- **이유:** 각 영역의 실무 생산성을 극대화하기 위함. 단, 의존성 설치 및 전역 제어는 최상위 루트 터미널로 일원화하여 싱글 락파일(`pnpm-lock.yaml`) 유지.

## 3. 인프라 비용 0원 통제 및 무설치 개발 환경 (Cloud-First)
- **결정:** 로컬 PC에 MySQL/Redis를 설치하지 않고, 영구 무료 클라우드(Aiven MySQL, Upstash Serverless Redis) 연동
- **이유:** Windows 환경의 Redis 미지원 리스크 제거 및 로컬 환경 비대화 방지.

## 4. 초기 트러블슈팅 이력 요약
- **pnpm v11 네이티브 바이너리 빌드 차단:** `.npmrc` 보안 정책 충돌로 Next.js 생성 실패 -> `pnpm approve-builds` 및 쉼표 분리형 문자열 규격으로 허용 조치 완료.
- **GitHub Actions YAML 문법 오류:** pnpm 버전 인자가 숫자로 파싱되어 가상 컴퓨터 다운 -> 명시적 문자열 '11.3.0'으로 수술적 교정 완료.
- **CI 빌드 중 하이버네이트 DB 연결 실패:** 가상 환경 내 MySQL 인스턴스 부재로 통합 테스트 단계에서 빌드 차단 -> testRuntimeOnly H2 의존성 주입 및 테스트 전용 격리 application.yaml 환경 설정을 분리하여 해결 완료.
- **GitHub Actions 노션 환경 변수 누락:** NOTION_TOKEN 및 DATABASE_ID 참조 실패로 node 스크립트 가동 중단 -> GitHub Repository Secrets 세팅 내 암호화 키 바인딩 완료.

## 5. API 명세서 문서화 자동화
- **결정:** Springdoc OpenAPI 사양 자동 추출 및 빌드 시점 내 순수 Node.js 스크립트를 통한 Notion 데이터베이스 동기화 구축.
- **이유:** 백엔드 코드 변경 사항이 문서에 실시간 반영되도록 하여 프론트엔드 개발자와의 소통 비용 및 사양 불일치 리스크를 격리함.

## 6. 실시간 인구 혼잡도 데이터 가상화 및 공용 타입 정의
- **결정:** 서울시 실시간 도시데이터에서 필요한 인구 상태 구조만 추출하여 `mocks/congestion.json` 파일로 규격화하고, 이를 `packages/types/congestion.ts` 인터페이스와 연동함.
- **이유:** 외부 공공 API 서버의 가동 상태와 무관하게 컴파일 및 비즈니스 로직 검증이 가능한 무중단 개발 환경을 구축하기 위함임. 또한 카멜케이스(camelCase) 정제 및 4단계 혼잡도 지표(GREEN, YELLOW, ORANGE, RED) 상수화를 통해 프론트엔드와 백엔드의 데이터 해석 규격을 일치시킴.