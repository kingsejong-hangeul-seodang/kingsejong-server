#!/usr/bin/env bash
# 배포용 로그인 코드를 만든다 (회원가입이 없어서 팀이 코드를 나눠 준다).
#   scripts/create-accounts.sh 10            → 이름 "학습자 1~10", 코드 DGU-XXXXXX 10개
#   scripts/create-accounts.sh 3 "시연"      → 이름 "시연 1~3"
# 코드는 헷갈리는 글자(0 O 1 I)를 뺀 무작위 6자리. 만든 코드는 화면에 한 번만 보이니 따로 적어 둔다.
# 서버가 한 번은 떠서 users 테이블이 있어야 한다.
set -euo pipefail
cd "$(dirname "$0")/.."

COUNT="${1:-10}"
NAME="${2:-학습자}"
if ! [[ "$COUNT" =~ ^[0-9]+$ ]] || [ "$COUNT" -lt 1 ] || [ "$COUNT" -gt 200 ]; then
  echo "개수는 1~200: scripts/create-accounts.sh 10" >&2
  exit 1
fi
if [ "${#NAME}" -gt 20 ]; then
  echo "이름은 20자 이하로" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
. ./.env
set +a

# 32글자(헷갈리는 0 O 1 I 뺌). 무작위 바이트 % 32 라 고르게 나온다
ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
values=""
for i in $(seq 1 "$COUNT"); do
  code="DGU-"
  for byte in $(od -An -N6 -tu1 /dev/urandom); do
    code+="${ALPHABET:$((byte % 32)):1}"
  done
  name="${NAME//\'/} ${i}"
  values+="${values:+,}('${name}', '${code}', 'KO', false, false, 0, 0, 0, now(), now())"
done

docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -q -At \
  -U "${DB_USER:-kingsejong}" -d "${DB_NAME:-kingsejong}" <<SQL
insert into users (name, login_code, locale, consent_agreed, onboarding_completed,
                   exp, streak_days, correct_streak, created_at, updated_at)
values ${values}
on conflict (login_code) do nothing
returning name || '  ' || login_code;
SQL
