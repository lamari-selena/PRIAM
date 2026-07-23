#!/bin/sh
# One-off backfill for PRIAM data_subject/processed_data rows (playbook
# §4bis, last point) - NOT a permanent application endpoint. Covers only
# sessions that already had a cart in Redis BEFORE register_data_subject()/
# reportProcessedData() were wired into frontend/middleware.go and
# frontend/handlers.go.
#
# Reuses the application's own real Redis instance directly (SCAN over
# redis-cart) rather than a separate database-to-database access -
# every key in this Redis instance already IS a real idRef (session_id),
# since cartservice keys its store by nothing else. Every OnlineBoutique
# "Cart" data_id (1=product_id, 2=quantity, see
# Databases/db_insertion_script.sql) is reported unconditionally per key
# found: this application has only one DataType, so there is no ambiguity
# about which data_ids a given subject "holds" once it has any cart key at
# all.
#
# Run from the repo root, with the OnlineBoutique + PRIAM stacks already up:
#   sh case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh
#
# Idempotent: register_data_subject (POST /api/DataSubject) upserts by idRef
# (DataSubjectServiceImpl.saveDataSubject, playbook §4bis) - safe to re-run.

set -eu

PRIAM_ACTOR_URL="${PRIAM_ACTOR_URL:-http://localhost:8082}"
PRIAM_DATA_URL="${PRIAM_DATA_URL:-http://localhost:8081}"
REDIS_CONTAINER="${REDIS_CONTAINER:-ob-redis-cart}"
DATA_SUBJECT_CATEGORY_ID=1

echo "Scanning $REDIS_CONTAINER for existing session_id keys..."
KEYS=$(docker exec "$REDIS_CONTAINER" redis-cli --scan)

if [ -z "$KEYS" ]; then
  echo "No pre-existing cart keys found - nothing to backfill."
  exit 0
fi

echo "$KEYS" | while IFS= read -r idRef; do
  [ -z "$idRef" ] && continue
  echo "--- backfilling idRef=$idRef ---"

  curl -s -o /dev/null -w "  register_data_subject -> HTTP %{http_code}\n" \
    -X POST "$PRIAM_ACTOR_URL/api/DataSubject" \
    -H "Content-Type: application/json" \
    -d "{\"idRef\":\"$idRef\",\"dataSubjectCategoryId\":$DATA_SUBJECT_CATEGORY_ID}"

  dataSubjectId=$(curl -s "$PRIAM_ACTOR_URL/api/DataSubjectId/$idRef")
  echo "  dataSubjectId=$dataSubjectId"

  curl -s -o /dev/null -w "  report_processed_data -> HTTP %{http_code}\n" \
    -X POST "$PRIAM_DATA_URL/api/processed-data/add?subjectId=$dataSubjectId" \
    -H "Content-Type: application/json" \
    -d "[1,2]"
done

echo "Backfill complete."
