#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

if [[ -f "${PROJECT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${PROJECT_DIR}/.env"
  set +a
fi

DATASET_ROWS="${1:-}"
if [[ -z "$DATASET_ROWS" || ! "$DATASET_ROWS" =~ ^[0-9]+$ || "$DATASET_ROWS" -le 0 ]]; then
  echo "Uzycie: $0 <liczba_rekordow>"
  echo "Przyklad: $0 1000000"
  exit 1
fi

: "${SCYLLA_KEYSPACE:=bench}"
: "${SCYLLA_USER:=cassandra}"
: "${SCYLLA_PWD:=cassandra}"
: "${SCYLLA_COPY_RETRIES:=3}"

cd "$PROJECT_DIR"

echo "[SCYLLA] Uruchamiam kontener..."
docker compose up -d scylla

echo "[SCYLLA] Czekam na gotowosc..."
wait_for_scylla_ready() {
  until docker compose exec -T scylla /usr/bin/cqlsh -u "$SCYLLA_USER" -p "$SCYLLA_PWD" -e "DESCRIBE KEYSPACES" >/dev/null 2>&1; do
    sleep 3
  done
}

wait_for_scylla_ready

echo "[SCYLLA] Czyszcze keyspace ${SCYLLA_KEYSPACE}..."
docker compose exec -T scylla /usr/bin/cqlsh -u "$SCYLLA_USER" -p "$SCYLLA_PWD" -e "DROP KEYSPACE IF EXISTS ${SCYLLA_KEYSPACE};"

echo "[SCYLLA] Wgrywam schemat..."
docker compose cp "schema/widecolumn/create_schema.cql" scylla:/tmp/create_schema.cql
docker compose exec -T scylla /usr/bin/cqlsh -u "$SCYLLA_USER" -p "$SCYLLA_PWD" -f /tmp/create_schema.cql

echo "[SCYLLA] Generuje dane bulk (${DATASET_ROWS})..."
BULK_DIR="generated_sql/bulk_scylla_${DATASET_ROWS}"
REQUIRED_FILES=(
  "bookshops.csv"
  "books_by_shop.csv"
  "users.csv"
  "user_credentials_by_login.csv"
  "employees_by_shop.csv"
  "rentals_by_user.csv"
  "rentals_by_shop.csv"
  "reservations_by_user.csv"
  "load_scylla_copy.cql"
)

missing_files=()
for required_file in "${REQUIRED_FILES[@]}"; do
  if [[ ! -f "${BULK_DIR}/${required_file}" ]]; then
    missing_files+=("$required_file")
  fi
done

if [[ "${#missing_files[@]}" -gt 0 ]]; then
  echo "[SCYLLA] Brakuje plikow bulk: ${missing_files[*]}"
  echo "[SCYLLA] Regeneruje pakiet bulk dla ${DATASET_ROWS} rekordow..."
  python3 data_generator/generate_inserts.py -total-rows "$DATASET_ROWS" -engine scylla -nosql-load-mode bulk
else
  echo "[SCYLLA] Pakiet bulk kompletny - pomijam regeneracje."
fi

for required_file in "${REQUIRED_FILES[@]}"; do
  if [[ ! -f "${BULK_DIR}/${required_file}" ]]; then
    echo "[SCYLLA][ERROR] Nadal brakuje pliku po regeneracji: ${BULK_DIR}/${required_file}"
    exit 2
  fi
done

echo "[SCYLLA] Kopiuje paczke bulk do kontenera..."
docker compose exec -T scylla /bin/bash -lc 'rm -rf /tmp/bench_bulk && mkdir -p /tmp/bench_bulk'
docker compose cp "${BULK_DIR}/." scylla:/tmp/bench_bulk

echo "[SCYLLA] Wgrywam dane przez COPY..."

BASE_COPY_INGESTRATE=5000
BASE_COPY_CHUNKSIZE=200
BASE_COPY_MAXBATCHSIZE=8
BASE_COPY_MAXATTEMPTS=25

if [[ "$DATASET_ROWS" -ge 10000000 ]]; then
  # Dla 10M i wiecej schodzimy z tempem, zeby ograniczyc restarty i timeouty.
  BASE_COPY_INGESTRATE=1500
  BASE_COPY_CHUNKSIZE=80
  BASE_COPY_MAXBATCHSIZE=4
  BASE_COPY_MAXATTEMPTS=40
fi

if [[ -n "${SCYLLA_COPY_INGESTRATE:-}" ]]; then
  BASE_COPY_INGESTRATE="$SCYLLA_COPY_INGESTRATE"
fi
if [[ -n "${SCYLLA_COPY_CHUNKSIZE:-}" ]]; then
  BASE_COPY_CHUNKSIZE="$SCYLLA_COPY_CHUNKSIZE"
fi
if [[ -n "${SCYLLA_COPY_MAXBATCHSIZE:-}" ]]; then
  BASE_COPY_MAXBATCHSIZE="$SCYLLA_COPY_MAXBATCHSIZE"
fi
if [[ -n "${SCYLLA_COPY_MAXATTEMPTS:-}" ]]; then
  BASE_COPY_MAXATTEMPTS="$SCYLLA_COPY_MAXATTEMPTS"
fi

build_runtime_copy_script() {
  local ingest_rate="$1"
  local chunk_size="$2"
  local max_batch_size="$3"
  local max_attempts="$4"

  docker compose exec -T scylla /bin/bash -lc "\
    cp /tmp/bench_bulk/load_scylla_copy.cql /tmp/bench_bulk/load_scylla_copy.runtime.cql && \
    sed -i -E 's/INGESTRATE = [0-9]+/INGESTRATE = ${ingest_rate}/g; s/CHUNKSIZE = [0-9]+/CHUNKSIZE = ${chunk_size}/g; s/MAXBATCHSIZE = [0-9]+/MAXBATCHSIZE = ${max_batch_size}/g; s/MAXATTEMPTS = [0-9]+/MAXATTEMPTS = ${max_attempts}/g' /tmp/bench_bulk/load_scylla_copy.runtime.cql"
}

run_copy_with_retries() {
  local attempt=1
  local ingest_rate="$BASE_COPY_INGESTRATE"
  local chunk_size="$BASE_COPY_CHUNKSIZE"
  local max_batch_size="$BASE_COPY_MAXBATCHSIZE"
  local max_attempts="$BASE_COPY_MAXATTEMPTS"

  while [[ "$attempt" -le "$SCYLLA_COPY_RETRIES" ]]; do
    echo "[SCYLLA] COPY proba ${attempt}/${SCYLLA_COPY_RETRIES} (ingestrate=${ingest_rate}, chunksize=${chunk_size}, maxbatchsize=${max_batch_size}, maxattempts=${max_attempts})"
    build_runtime_copy_script "$ingest_rate" "$chunk_size" "$max_batch_size" "$max_attempts"

    if docker compose exec -T scylla /usr/bin/cqlsh -u "$SCYLLA_USER" -p "$SCYLLA_PWD" -f /tmp/bench_bulk/load_scylla_copy.runtime.cql; then
      return 0
    fi

    echo "[SCYLLA][WARN] COPY nieudany w probie ${attempt}."
    docker compose ps scylla || true
    docker compose logs --tail=40 scylla || true

    if [[ "$attempt" -eq "$SCYLLA_COPY_RETRIES" ]]; then
      return 1
    fi

    ingest_rate=$(( ingest_rate / 2 ))
    if [[ "$ingest_rate" -lt 200 ]]; then
      ingest_rate=200
    fi

    chunk_size=$(( chunk_size / 2 ))
    if [[ "$chunk_size" -lt 30 ]]; then
      chunk_size=30
    fi

    max_batch_size=$(( max_batch_size / 2 ))
    if [[ "$max_batch_size" -lt 2 ]]; then
      max_batch_size=2
    fi

    echo "[SCYLLA] Czekam az kontener znow bedzie gotowy..."
    wait_for_scylla_ready
    attempt=$(( attempt + 1 ))
  done

  return 1
}

if ! run_copy_with_retries; then
  echo "[SCYLLA][ERROR] COPY zakonczyl sie bledem po ${SCYLLA_COPY_RETRIES} probach."
  exit 3
fi

echo "[SCYLLA] Kontrola po imporcie (lekka walidacja)..."
EXPECTED_USERS=$(( DATASET_ROWS / 10 ))
if [[ "$EXPECTED_USERS" -lt 100 ]]; then
  EXPECTED_USERS=100
fi

USERS_CSV="generated_sql/bulk_scylla_${DATASET_ROWS}/users.csv"
if [[ -f "$USERS_CSV" ]]; then
  ACTUAL_USERS=$(( $(wc -l < "$USERS_CSV") - 1 ))
  echo "[SCYLLA] users.csv: oczekiwano ${EXPECTED_USERS}, wygenerowano ${ACTUAL_USERS}"
  if [[ "$ACTUAL_USERS" -ne "$EXPECTED_USERS" ]]; then
    echo "[SCYLLA][ERROR] Niezgodna liczba rekordow w users.csv"
    exit 2
  fi
else
  echo "[SCYLLA][ERROR] Brak pliku $USERS_CSV"
  exit 2
fi

docker compose exec -T scylla /usr/bin/cqlsh -u "$SCYLLA_USER" -p "$SCYLLA_PWD" -e "SELECT user_id FROM ${SCYLLA_KEYSPACE}.users LIMIT 1;" >/dev/null
echo "[SCYLLA] Walidacja OK (dane obecne, bez ciezkiego COUNT(*))"

echo "[SCYLLA] GOTOWE"
