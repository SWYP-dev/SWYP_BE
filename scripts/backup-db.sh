#!/usr/bin/env bash
set -euo pipefail

# 실행 위치: EC2 (cron 등록)
# swyp-mysql 컨테이너를 mysqldump로 덤프해 로컬에 보관하고, 보관 기간 지난 백업은 삭제

MYSQL_CONTAINER="swyp-mysql"
MYSQL_DATABASE="chwihap"
MYSQL_USER="root"
BACKUP_DIR="/home/ubuntu/backups/mysql"
RETENTION_DAYS=7

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD 환경변수가 필요합니다}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
BACKUP_FILE="$BACKUP_DIR/${MYSQL_DATABASE}_${TIMESTAMP}.sql.gz"

echo "[backup-db] dumping $MYSQL_DATABASE from $MYSQL_CONTAINER"
docker exec "$MYSQL_CONTAINER" \
  mysqldump -u"$MYSQL_USER" -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers "$MYSQL_DATABASE" \
  | gzip > "$BACKUP_FILE"

echo "[backup-db] created $BACKUP_FILE ($(du -h "$BACKUP_FILE" | cut -f1))"

echo "[backup-db] removing backups older than $RETENTION_DAYS days"
find "$BACKUP_DIR" -name "${MYSQL_DATABASE}_*.sql.gz" -mtime "+$RETENTION_DAYS" -delete

echo "[backup-db] done"
