#!/usr/bin/env bash
# 교정 엔진의 정답 획 사전을 만든다 (서버에서 한 번, 엔진의 획 추출 코드가 바뀌면 다시).
# 폰트(동국체)는 git에 없어서 deploy/engine-data/fonts/DONGGUK-UNIVERSITY.ttf 에 직접 올려 둔다.
# 엔진 컨테이너 안에서 extract → compose 를 돌려 deploy/engine-data/extracted/ 에 사전을 남긴다 (2~7분).
#
# 노트북에서 이미 만든 사전이 있으면 이 스크립트 대신 그 폴더를 올려도 된다:
#   scp -r Python-correction-engine/data/extracted/DONGGUK-UNIVERSITY \
#       ubuntu@서버:~/kingsejong/kingsejong-server/deploy/engine-data/extracted/
set -euo pipefail
cd "$(dirname "$0")/.."

FONT=engine-data/fonts/DONGGUK-UNIVERSITY.ttf
if [ ! -f "$FONT" ]; then
  echo "폰트가 없어요: deploy/$FONT 에 동국체 ttf를 올려 주세요" >&2
  exit 1
fi

docker compose build engine
docker compose run --rm --no-deps --user root engine sh -c \
  "python -m app.core.stroke_vectorizer.extract --hook-len 22 && python -m app.core.stroke_vectorizer.compose --all"

# 떠 있는 엔진이 새 사전을 읽게 다시 켠다
if [ -n "$(docker compose ps -q engine)" ]; then
  docker compose restart engine
fi
echo "정답 획 사전을 만들었어요: deploy/engine-data/extracted/DONGGUK-UNIVERSITY"
