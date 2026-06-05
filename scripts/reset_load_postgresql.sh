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

: "${POSTGRES_USER:=postgres}"
: "${POSTGRES_DB:=bench}"

cd "$PROJECT_DIR"

echo "[POSTGRESQL] Uruchamiam kontener..."
docker compose up -d postgres

echo "[POSTGRESQL] Czekam na gotowosc..."
until docker compose exec -T postgres pg_isready -d "$POSTGRES_DB" -U "$POSTGRES_USER" >/dev/null 2>&1; do
  sleep 2
done

echo "[POSTGRESQL] Czyszcze schema bench..."
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" \
  -c "DROP SCHEMA IF EXISTS bench CASCADE; CREATE SCHEMA bench;"

echo "[POSTGRESQL] Wgrywam strukture tabel..."
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /dev/stdin < schema/sql/create_schema_structure.sql

echo "[POSTGRESQL] Generuje dane bulk (${DATASET_ROWS})..."
python3 data_generator/generate_inserts.py -total-rows "$DATASET_ROWS" -engine postgresql -relational-load-mode bulk

echo "[POSTGRESQL] Kopiuje paczke bulk do kontenera..."
docker compose exec -T postgres bash -lc 'rm -rf /tmp/bench_bulk && mkdir -p /tmp/bench_bulk'
docker compose cp "generated_sql/bulk_postgresql_${DATASET_ROWS}/." postgres:/tmp/bench_bulk

echo "[POSTGRESQL] Wgrywam dane przez COPY..."
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -f /tmp/bench_bulk/load_postgresql_copy.sql

echo "[POSTGRESQL] Kontrola liczby uzytkownikow..."
docker compose exec -T postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT COUNT(*) AS users_count FROM bench.bookshopuser;"

echo "[POSTGRESQL] GOTOWE"
