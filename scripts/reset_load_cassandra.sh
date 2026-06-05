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

: "${CASSANDRA_KEYSPACE:=bench}"
: "${CASSANDRA_USER:=cassandra}"
: "${CASSANDRA_PWD:=cassandra}"

cd "$PROJECT_DIR"

echo "[CASSANDRA] Uruchamiam kontener..."
docker compose up -d cassandra

echo "[CASSANDRA] Czekam na gotowosc..."
until docker compose exec -T cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -e "DESCRIBE KEYSPACES" >/dev/null 2>&1; do
  sleep 3
done

echo "[CASSANDRA] Czyszcze keyspace ${CASSANDRA_KEYSPACE}..."
docker compose exec -T cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -e "DROP KEYSPACE IF EXISTS ${CASSANDRA_KEYSPACE};"

echo "[CASSANDRA] Wgrywam schemat..."
docker compose cp "schema/widecolumn/create_schema.cql" cassandra:/tmp/create_schema.cql
docker compose exec -T cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /tmp/create_schema.cql

echo "[CASSANDRA] Generuje dane bulk (${DATASET_ROWS})..."
python3 data_generator/generate_inserts.py -total-rows "$DATASET_ROWS" -engine cassandra -nosql-load-mode bulk

echo "[CASSANDRA] Kopiuje paczke bulk do kontenera..."
docker compose exec -T cassandra /bin/bash -lc 'rm -rf /tmp/bench_bulk && mkdir -p /tmp/bench_bulk'
docker compose cp "generated_sql/bulk_cassandra_${DATASET_ROWS}/." cassandra:/tmp/bench_bulk

echo "[CASSANDRA] Wgrywam dane przez COPY..."
docker compose exec -T cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /tmp/bench_bulk/load_cassandra_copy.cql

echo "[CASSANDRA] Kontrola po imporcie (lekka walidacja)..."
EXPECTED_USERS=$(( DATASET_ROWS / 10 ))
if [[ "$EXPECTED_USERS" -lt 100 ]]; then
  EXPECTED_USERS=100
fi

USERS_CSV="generated_sql/bulk_cassandra_${DATASET_ROWS}/users.csv"
if [[ -f "$USERS_CSV" ]]; then
  ACTUAL_USERS=$(( $(wc -l < "$USERS_CSV") - 1 ))
  echo "[CASSANDRA] users.csv: oczekiwano ${EXPECTED_USERS}, wygenerowano ${ACTUAL_USERS}"
  if [[ "$ACTUAL_USERS" -ne "$EXPECTED_USERS" ]]; then
    echo "[CASSANDRA][ERROR] Niezgodna liczba rekordow w users.csv"
    exit 2
  fi
else
  echo "[CASSANDRA][ERROR] Brak pliku $USERS_CSV"
  exit 2
fi

docker compose exec -T cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -e "SELECT user_id FROM ${CASSANDRA_KEYSPACE}.users LIMIT 1;" >/dev/null
echo "[CASSANDRA] Walidacja OK (dane obecne, bez ciezkiego COUNT(*))"

echo "[CASSANDRA] GOTOWE"
