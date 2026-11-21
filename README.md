# kingsejong-server

킹세종 한글서당 백엔드 서버

## 기술 스택

- Java 21, Spring Boot 3.5
- PostgreSQL 16 (Docker), Spring Data JPA
- JUnit 5, Testcontainers
- springdoc (Swagger UI)

## 로컬 실행

Docker가 필요합니다.

```bash
docker compose up -d --wait
./gradlew bootRun
```

- API: http://localhost:8080/api/v1
- 헬스체크: http://localhost:8080/api/v1/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## 폴더 구조

```
src/main/java/com/kingsejong
├── domain          # 기능별 패키지
│   └── {기능}
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       └── dto
└── global          # 공통 설정, 오류 처리, 헬스체크
```

## 협업 컨벤션

| | 형식 | 예 |
| --- | --- | --- |
| 브랜치 | `태그/#이슈번호` | `feat/#12` |
| 커밋 | `태그: 내용` | `Feat: 로그인 API 추가` |
| PR | `이모지 태그 #이슈번호 - 제목` → `dev` | `✨ Feat #12 - 로그인 API` |

태그: `Feat` ✨ · `Fix` 🐛 · `Refactor` ♻️ · `Docs` 📝 · `Chore` 🔧
