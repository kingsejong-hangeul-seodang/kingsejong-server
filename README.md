# kingsejong-server

킹세종 한글서당 백엔드 (Spring Boot). 프론트는 이 서버만 부르고, 교정 엔진(Python FastAPI)은 이 서버만 부릅니다.

## 기술 스택

| 항목 | 값 |
| --- | --- |
| Java | 21 — 로컬에 없어도 Gradle이 자동으로 받음 |
| Spring Boot | 3.5.16 (Gradle 8.14.5 wrapper 포함) |
| DB | PostgreSQL 16 (Docker). 스키마는 JPA `ddl-auto: update` |
| 테스트 | JUnit 5 + Testcontainers (실제 PostgreSQL) |
| 문서 | springdoc (Swagger UI) |
| 기타 | Validation, Actuator, Lombok |

## 로컬 실행

필요한 것: **Docker** (Desktop / Colima / OrbStack 아무거나)

```bash
docker compose up -d --wait     # PostgreSQL 기동 (호스트 포트 5433)
./gradlew bootRun               # 프로필 기본값 local
```

> `docker: unknown command: docker compose`가 나오면 Compose가 따로 설치된 경우예요. `docker-compose up -d --wait`처럼 하이픈을 붙여서 쓰면 돼요.

| 주소 | 내용 |
| --- | --- |
| `GET http://localhost:8080/api/v1/health` | 헬스체크 |
| `GET http://localhost:8080/actuator/health` | DB 포함 상세 상태 |
| http://localhost:8080/swagger-ui.html | Swagger UI |
| `localhost:5433` | DB 접속 (db `kingsejong` / user `kingsejong` / pw `kingsejong1234`) |

IntelliJ: Project Structure → SDK 21 지정 (없으면 Download JDK → Temurin 21).

| 자주 쓰는 명령 | 설명 |
| --- | --- |
| `./gradlew build` | 빌드 + 테스트 (CI와 같음, Docker 필요) |
| `docker compose down -v` | DB 초기화. 다음 부팅 때 JPA가 테이블을 다시 만듦 |
| 포트를 바꾸고 싶을 때 | `.env.example`을 `.env`로 복사해서 `DB_PORT` 수정 |

## 패키지 구조 — 3계층 (Controller → Service → Repository)

```
com.kingsejong
├── domain                     # 기능별 패키지
│   └── example                # 구조 예시 (실제 개발 시작하면 삭제)
│       ├── controller         # 요청·응답, 검증 (@Valid)
│       ├── service            # 비즈니스 로직, 트랜잭션
│       ├── repository         # DB 접근 (JpaRepository)
│       ├── entity             # JPA 엔티티
│       └── dto
│           ├── request
│           └── response
└── global                     # 공통 코드
    ├── config                 # JPA, Swagger, CORS
    ├── common                 # BaseTimeEntity
    ├── error                  # ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
    └── health                 # 헬스체크
```

## API 규칙

전체 명세는 노션 "API명세서"에 있습니다.

- 기본 주소 `/api/v1`, 리소스는 복수형 (`/contents`), 내 것은 `/me`
- 필드 이름 camelCase, 시간은 ISO 8601 `+09:00`
- 성공 응답은 감싸지 않고 데이터 그대로 (조회 200, 만들기 201, 내용 없음 204)
- 오류 응답은 항상 `{ "code", "message", "details" }`. 컨트롤러에서 try-catch 하지 말고 `BusinessException(ErrorCode.xxx)`을 던진다
- 요청 검증 실패는 422 `INVALID_REQUEST`, 깨진 JSON은 400 `INVALID_JSON`
- 다른 도메인의 데이터가 필요하면 그쪽 Repository가 아니라 Service를 주입한다
- 엔티티를 그대로 응답하지 않고 DTO로 바꾼다

## 협업

브랜치·커밋·이슈·PR 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 봐 주세요.

## 할 일 (인프라)

- [ ] 로그인 (JWT) · 인증 필터
- [ ] 교정 엔진 연동 (`ENGINE_BASE_URL`, 헬스체크에 엔진 상태 추가)
- [ ] Dockerfile · 배포
