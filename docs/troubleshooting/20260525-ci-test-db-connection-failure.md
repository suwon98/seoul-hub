# CI 빌드 단계 내 하이버네이트 디비 커넥션 및 다이얼렉트 판별 실패

### 1. 현상 (Issue)
- 깃허브 액션즈 파이프라인 구동 중 Backend CI의 테스트 단계에서 빌드 실패 발생.
- BackendApplicationTests > contextLoads() FAILED 예외 및 HibernateException (DialectFactoryImpl) 로그 출력.

### 2. 원인 분석 (Root Cause)
- @SpringBootTest가 가동되며 전체 애플리케이션 컨텍스트를 로드할 때 실제 데이터베이스 커넥션을 요구함.
- 메인 자산인 application.yaml은 클라우드 환경 변수 또는 로컬 MySQL 설정을 바라보고 있으나, 깃허브 액션즈 가상 환경 내에는 가용 데이터베이스 인스턴스가 존재하지 않아 접속 지연 및 다이얼렉트 빌드 에러로 연쇄 차단됨.

### 3. 해결 방안 (Resolution)
- 백엔드 build.gradle의 testRuntimeOnly 스펙에 com.h2database:h2 라이브러리 추가하여 테스트 시 가상 디비 인프라 배치.
- apps/backend/src/test/resources/application.yaml 격리 프로필 설정을 생성하여 테스트 진입 시점에 H2 인메모리 드라이버 및 H2Dialect를 강제 주입하도록 아키텍처 보완.

### 4. 재발 방지 및 교훈 (Prevention & Lesson)
- 모노레포 및 CI/CD 환경을 구축할 때는 빌드 머신의 외부 자원 종속성을 원천 차단해야 함. 테스트 환경 설정 자산을 메인 소스코드와 완벽히 격리함으로써 인프라 비용 소모 없이 유효성 검증 체계를 유지함.