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

쓰기 제출(채점)에는 교정 엔진([Python-correction-engine](https://github.com/kingsejong-hangeul-seodang/Python-correction-engine))이 `localhost:8000`에 떠 있어야 합니다. 엔진 레포에서:

```bash
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn app.main:app --port 8000
```

다른 주소면 환경 변수 `ENGINE_BASE_URL`로 바꿉니다.

콘텐츠는 `src/main/resources/seed/contents.json`이 원본입니다. 고치고 서버를 다시 켜면 반영되고, DB에서 직접 고친 값은 덮어써집니다.

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
├── global          # 공통 설정, 인증, 오류 처리, 시드, 헬스체크
└── infra           # 외부 시스템 연동 (교정 엔진)
```

## 협업 컨벤션

| | 형식 | 예 |
| --- | --- | --- |
| 브랜치 | `태그/#이슈번호` | `feat/#12` |
| 커밋 | `태그: 내용` | `Feat: 로그인 API 추가` |
| PR | `이모지 태그 #이슈번호 - 제목` → `dev` | `✨ Feat #12 - 로그인 API` |

태그: `Feat` ✨ · `Fix` 🐛 · `Refactor` ♻️ · `Docs` 📝 · `Chore` 🔧
