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
- **pnpm v11 네이티브 바이너리 빌드 차단:** `.npmrc` 보안 정책 충돌로 Next.js 생성 실패 $\rightarrow$ `pnpm approve-builds` 및 쉼표 분리형 문자열 규격으로 허용 조치 완료.
- **GitHub Actions YAML 문법 오류:** pnpm 버전 인자가 숫자로 파싱되어 가상 컴퓨터 다운 $\rightarrow$ 명시적 문자열 `'11.3.0'`으로 수술적 교정 완료.