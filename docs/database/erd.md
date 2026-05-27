# 데이터베이스 ERD 명세 (Entity Relationship Diagram)

```mermaid
erDiagram
    users {
        BIGINT id PK "Auto Increment"
        VARCHAR email "Unique, 로그인 이메일"
        VARCHAR password "암호화된 비밀번호"
        VARCHAR role "USER, ADMIN 권한분기"
        DATETIME created_at
    }

    congestion_records {
        BIGINT id PK "Auto Increment"
        VARCHAR area_name "서울시 주요 지역명"
        VARCHAR congestion_level "GREEN, YELLOW, ORANGE, RED"
        VARCHAR congestion_message "VARCHAR(500) 상세 메시지"
        INT population_min
        INT population_max
        DATETIME update_time
    }

    questions {
        BIGINT id PK "Auto Increment"
        BIGINT user_id FK "users.id 참조"
        VARCHAR area_name "질문 대상 지역 필터링용"
        VARCHAR title "질문 제목"
        TEXT content "질문 본문"
        DATETIME created_at
    }

    answers {
        BIGINT id PK "Auto Increment"
        BIGINT question_id FK "questions.id 참조 (Cascade Delete)"
        BIGINT user_id FK "users.id 참조"
        TEXT content "답변 댓글 본문"
        DATETIME created_at
    }

    users ||--o{ questions : "작성한다 (1:N)"
    users ||--o{ answers : "작성한다 (1:N)"
    questions ||--o{ answers : "포함한다 (1:N)"