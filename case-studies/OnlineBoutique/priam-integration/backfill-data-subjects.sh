#!/bin/sh
# One-off backfill for PRIAM data_subject/processed_data rows (playbook
# §4bis, last point) - NOT a permanent application endpoint. Covers only
# accounts that already existed in onlineboutique.db BEFORE
# registerDataSubject()/reportProcessedData() were wired into
# frontend/accounts_handlers.go and frontend/handlers.go.
#
# Reuses the application's own real SQLite file directly (a throwaway
# `alpine`+`sqlite` container reading the same bind-mounted volume the
# `frontend` container writes to - see docker-compose.yml's
# `onlineboutique-db` volume) rather than a separate database-to-database
# access, then calls PRIAM's own public HTTP API from the host for each row
# found, exactly like a real sign-up/checkout would.
#
# Run from the repo root, with the OnlineBoutique + PRIAM stacks already up:
#   sh case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh
#
# Idempotent: registerDataSubject (POST /api/DataSubject) upserts by idRef
# (DataSubjectServiceImpl.saveDataSubject, playbook §4bis) - safe to re-run.
# reportProcessedData increments an occurrence counter per call
# (ProcessedData.nbOccurrences) - re-running will over-count if orders were
# already reported by a previous run of this same script or by real traffic
# in the meantime; harmless for the Access Request list (still shows the
# field once), but not perfectly idempotent for that counter.

# -e deliberately NOT set: under real testing (Windows/Cygwin `sh` talking
# to Docker Desktop), `set -e` combined with the `docker run`/curl calls
# below caused the script to abort silently mid-loop after the first
# iteration's first command, even though every individual command exited 0
# when run standalone - never fully root-caused (did not reproduce on a
# plain Linux shell), but dropping -e made the exact same logic complete
# correctly for every user/order found, verified against real state. -u
# stays on to still catch a genuine unset-variable typo.
set -u

PRIAM_ACTOR_URL="${PRIAM_ACTOR_URL:-http://localhost:8082}"
PRIAM_DATA_URL="${PRIAM_DATA_URL:-http://localhost:8081}"
DB_VOLUME="${DB_VOLUME:-$(cd "$(dirname "$0")/../../.." && pwd)/onlineboutique-db-volume}"
DATA_SUBJECT_CATEGORY_ID=1
USER_DATA_IDS='[1]'
ORDER_DATA_IDS='[2,3,4,5,6,7,8]'

echo "Reading users from $DB_VOLUME/onlineboutique.db ..."
USER_IDS=$(docker run --rm -v "$DB_VOLUME:/data:ro" keinos/sqlite3 \
  sqlite3 /data/onlineboutique.db "SELECT id FROM users;")

if [ -z "$USER_IDS" ]; then
  echo "No pre-existing accounts found - nothing to backfill."
  exit 0
fi

# A `for` loop over word-split lines, not `echo ... | while read`: the
# piped form runs the loop body in a subshell on some `sh` implementations,
# which can silently swallow a mid-loop failure or stdin interaction from a
# command like `docker run` inside the loop body - observed under a
# Windows/Cygwin `sh` during this integration's own testing (the loop body
# stopped dead after the first `docker run` call, no error surfaced). A
# `for` loop has no such subshell, and default IFS already splits on
# newlines.
for idRef in $USER_IDS; do
  [ -z "$idRef" ] && continue
  echo "--- backfilling idRef=$idRef ---"

  curl -s -o /dev/null -w "  registerDataSubject -> HTTP %{http_code}\n" \
    -X POST "$PRIAM_ACTOR_URL/api/DataSubject" \
    -H "Content-Type: application/json" \
    -d "{\"idRef\":\"$idRef\",\"dataSubjectCategoryId\":$DATA_SUBJECT_CATEGORY_ID}"

  dataSubjectId=$(curl -s "$PRIAM_ACTOR_URL/api/DataSubjectId/$idRef")
  echo "  dataSubjectId=$dataSubjectId"

  curl -s -o /dev/null -w "  reportProcessedData(User) -> HTTP %{http_code}\n" \
    -X POST "$PRIAM_DATA_URL/api/processed-data/add?subjectId=$dataSubjectId" \
    -H "Content-Type: application/json" \
    -d "$USER_DATA_IDS"

  orderCount=$(docker run --rm -v "$DB_VOLUME:/data:ro" keinos/sqlite3 \
    sqlite3 /data/onlineboutique.db "SELECT COUNT(1) FROM orders WHERE user_id = '$idRef';")
  echo "  $orderCount pre-existing order(s)"

  i=0
  while [ "$i" -lt "$orderCount" ]; do
    curl -s -o /dev/null -w "  reportProcessedData(Order) -> HTTP %{http_code}\n" \
      -X POST "$PRIAM_DATA_URL/api/processed-data/add?subjectId=$dataSubjectId" \
      -H "Content-Type: application/json" \
      -d "$ORDER_DATA_IDS"
    i=$((i + 1))
  done
done

echo "Backfill complete."
