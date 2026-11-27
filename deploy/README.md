# 배포 (서버 + 교정 엔진 + DB, 도커 한 대)

한 대의 서버에 도커로 네 개를 띄웁니다.

| 컨테이너 | 하는 일 | 밖에서 보이나 |
| --- | --- | --- |
| `caddy` | HTTPS(인증서 자동) → 서버로 전달 | 80, 443만 열림 |
| `server` | Spring Boot (`prod` 프로필) | 아니요 (Caddy 뒤) |
| `engine` | 교정 엔진 FastAPI | 아니요 (서버만 부름) |
| `postgres` | PostgreSQL 16 | 아니요 |

- 엔진 레포는 고치지 않습니다. 서버 레포 옆에 클론해 두면 `deploy/engine.Dockerfile`이 그 소스로 이미지를 만듭니다.
- 동국체 폰트와 정답 획 사전은 git에 없어서 `deploy/engine-data/`에 따로 둡니다 (컨테이너에 붙임).
- 프론트는 따로 배포합니다 (Vercel 등). 프론트의 `VITE_API_BASE_URL`을 `https://<API_DOMAIN>`으로.

## 1. 서버 준비 (오라클 클라우드 무료)

1. 인스턴스: **Ampere A1 (ARM)**, Ubuntu 22.04 이상, 2 OCPU · 12GB 이상 권장 (무료 한도 4 OCPU · 24GB).
   AMD 1GB 무료 인스턴스는 엔진(numpy·scipy) + 자바 + DB를 같이 올리기에 메모리가 모자랍니다.
2. 공인 IP 고정: 인스턴스 → 연결된 VNIC → IP 주소 → 예약된 공인 IP.
3. 포트 열기 (두 군데 다):
   - 콘솔: VCN → 보안 목록 → 수신 규칙에 TCP 80, 443 (소스 `0.0.0.0/0`).
   - 서버 안 방화벽 (오라클 우분투는 iptables가 기본으로 막음):
     ```bash
     sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
     sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
     sudo netfilter-persistent save
     ```
4. 도커 설치:
   ```bash
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker $USER   # 다시 로그인
   ```

## 2. 코드와 값

```bash
mkdir -p ~/kingsejong && cd ~/kingsejong
git clone https://github.com/kingsejong-hangeul-seodang/kingsejong-server.git
git clone https://github.com/kingsejong-hangeul-seodang/Python-correction-engine.git
cd kingsejong-server/deploy
cp .env.example .env
```

`.env` 채우기 (무작위 값은 `openssl rand -hex 32`):

| 값 | 넣을 것 |
| --- | --- |
| `API_DOMAIN` | 도메인이 없으면 공인 IP로 `152-67-1-2.sslip.io` (점을 `-`로). 인증서가 자동으로 나옵니다 |
| `CORS_ALLOWED_ORIGINS` | 프론트 주소 (예: `https://kingsejong.vercel.app`). 여러 개면 쉼표 |
| `DB_PASSWORD`, `JWT_SECRET`, `ADMIN_KEY` | 무작위 값 |
| `GEMINI_API_KEY` | 킹세종 말풍선용. 없으면 정해 둔 문장 |

## 3. 정답 획 사전 (처음 한 번)

1. 동국체 폰트를 올립니다: `deploy/engine-data/fonts/DONGGUK-UNIVERSITY.ttf`
   ```bash
   scp "DONGGUK UNIVERSITY.ttf" ubuntu@<IP>:~/kingsejong/kingsejong-server/deploy/engine-data/fonts/DONGGUK-UNIVERSITY.ttf
   ```
2. 사전 만들기 (엔진 컨테이너 안에서 2~7분):
   ```bash
   scripts/engine-data.sh
   ```
   노트북에 이미 만든 사전이 있으면 `Python-correction-engine/data/extracted/DONGGUK-UNIVERSITY` 폴더를 `deploy/engine-data/extracted/`에 올려도 됩니다.

사전이 없으면 엔진이 정답 획을 못 찾아서 판정·획순 재생이 안 됩니다 (`REFERENCE_UNAVAILABLE`).

## 4. 띄우기

```bash
docker compose up -d --build      # 처음 빌드는 10분쯤 (ARM에서 gradle·pip)
docker compose ps                 # 모두 healthy / running
docker compose logs -f server     # 기동 확인
curl https://<API_DOMAIN>/api/v1/health
# {"status":"ok","engine":{"status":"ok","engineVersion":"0.3.0"}}
```

처음 뜰 때 서버가 테이블을 만들고 콘텐츠·취향·자모 시드를 넣습니다. 시드 계정(DGU-0001 등)은 local·test에만 있어서 배포에는 없습니다.

## 5. 로그인 코드 만들기

```bash
scripts/create-accounts.sh 10            # 학습자 1~10, DGU-XXXXXX
scripts/create-accounts.sh 3 "시연"      # 시연 1~3
```

코드는 이때 한 번만 화면에 나옵니다. 팀에 나눠 줄 표에 바로 옮겨 두세요.

## 6. 운영

| 할 일 | 명령 |
| --- | --- |
| 최신 코드로 다시 배포 | `scripts/update.sh` (서버 `dev`, 엔진 `main`. `SERVER_BRANCH=main scripts/update.sh`) |
| 엔진 획 추출이 바뀜 | `scripts/engine-data.sh` |
| DB 백업 | `scripts/backup-db.sh` (최근 14개). 매일: `crontab -e`에 `0 4 * * * ~/kingsejong/kingsejong-server/deploy/scripts/backup-db.sh` |
| 로그 | `docker compose logs -f server` / `engine` / `caddy` |
| 다시 켜기 | `docker compose restart server` |
| 전부 내리기 (DB는 남음) | `docker compose down` |

## 7. 알아 둘 것

- **`SPRING_PROFILES_ACTIVE=prod`가 꼭 들어가야 합니다.** 이미지와 compose 둘 다에 넣어 두었습니다. 빠지면 로컬 기본값(로컬 DB 주소, 공개된 관리자 키)으로 뜹니다.
- prod에서는 Swagger가 꺼져 있습니다. 관리자 API(유튜브 콘텐츠 만들기)는 프론트 개발 도구(`?tools=youtube`)를 `VITE_API_BASE_URL`을 배포 주소로 두고 쓰거나 curl로 부릅니다.
- **유튜브 자막 가져오기는 클라우드 IP에서 막힐 가능성이 큽니다.** 막히면 프론트 개발 도구에서 자막을 붙여 넣어 만들거나, 노트북(로컬)에서 만든 `yt:` 콘텐츠를 옮깁니다 (옮기는 스크립트는 넣을 영상이 정해지면 만듭니다).
- 로그인 시도 제한은 아직 없습니다. 코드가 무작위 6자리(32종 글자)라 맞히기 어렵지만, 공개 시연이 길어지면 넣습니다.
- 서버를 두 대 이상 띄우지 않습니다 (시드가 동시에 돌 수 있음).
- 이미 기록이 있는 DB를 옮겨 왔다면 획순 오류 칸 수를 한 번 채웁니다:
  ```sql
  update writings set stroke_error_count = (select count(*) from jsonb_array_elements(grade->'cells') c where c->>'mark' = 'stroke')
  where stroke_error_count is null and status = 'GRADED' and grade is not null;
  ```
