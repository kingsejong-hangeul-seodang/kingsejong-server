#!/usr/bin/env bash
# 최신 코드로 다시 배포한다: 서버·엔진 레포를 받고, 이미지를 다시 만들고, 바뀐 것만 다시 띄운다.
#   scripts/update.sh            → 서버 dev, 엔진 main
#   SERVER_BRANCH=main scripts/update.sh
# 엔진의 획 추출 코드가 바뀌었으면 scripts/engine-data.sh 로 사전도 다시 만든다.
set -euo pipefail
cd "$(dirname "$0")/.."

set -a
# shellcheck disable=SC1091
. ./.env
set +a

SERVER_BRANCH="${SERVER_BRANCH:-dev}"
ENGINE_BRANCH="${ENGINE_BRANCH:-main}"
ENGINE_DIR="${ENGINE_SRC:-../../Python-correction-engine}"

git -C .. fetch --quiet origin
git -C .. checkout --quiet "$SERVER_BRANCH"
git -C .. pull --quiet --ff-only origin "$SERVER_BRANCH"
git -C "$ENGINE_DIR" fetch --quiet origin
git -C "$ENGINE_DIR" checkout --quiet "$ENGINE_BRANCH"
git -C "$ENGINE_DIR" pull --quiet --ff-only origin "$ENGINE_BRANCH"

echo "서버 $(git -C .. log --oneline -1)"
echo "엔진 $(git -C "$ENGINE_DIR" log --oneline -1)"

docker compose build
docker compose up -d
docker image prune -f > /dev/null
docker compose ps
