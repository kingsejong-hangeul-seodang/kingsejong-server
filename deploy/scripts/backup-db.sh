#!/usr/bin/env bash
# DB를 deploy/backups/ 에 덤프한다 (14개만 남김). 매일 돌리려면 crontab -e:
#   0 4 * * * /home/ubuntu/kingsejong/kingsejong-server/deploy/scripts/backup-db.sh >> /tmp/kingsejong-backup.log 2>&1
# 되돌리기: gunzip -c backups/파일.sql.gz | docker compose exec -T postgres psql -U kingsejong -d kingsejong
set -euo pipefail
cd "$(dirname "$0")/.."

set -a
# shellcheck disable=SC1091
. ./.env
set +a

mkdir -p backups
file="backups/kingsejong-$(date +%Y%m%d-%H%M%S).sql.gz"
docker compose exec -T postgres pg_dump -U "${DB_USER:-kingsejong}" -d "${DB_NAME:-kingsejong}" --no-owner \
  | gzip > "$file"
echo "백업: deploy/$file"

# 오래된 백업 정리 (최근 14개만)
ls -1t backups/kingsejong-*.sql.gz | tail -n +15 | xargs -r rm --
