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

: "${MSSQL_DB:=bench}"

cd "$PROJECT_DIR"

get_mssql_password() {
  if [[ -n "${MSSQL_SA_PWD:-}" ]]; then
    printf '%s' "$MSSQL_SA_PWD"
    return
  fi
  docker compose exec -T mssql /bin/bash -lc 'printf %s "$MSSQL_SA_PASSWORD"' 2>/dev/null || true
}

MSSQL_PASSWORD="$(get_mssql_password)"
if [[ -z "$MSSQL_PASSWORD" ]]; then
  echo "Brak hasla MSSQL (MSSQL_SA_PWD lub MSSQL_SA_PASSWORD)."
  exit 1
fi

echo "[MSSQL] Uruchamiam kontener..."
docker compose up -d mssql

echo "[MSSQL] Czekam na gotowosc..."
until docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -Q "SELECT 1" >/dev/null 2>&1; do
  sleep 2
done

echo "[MSSQL] Usuwam i tworze baze ${MSSQL_DB}..."
docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -i /dev/stdin <<SQL
IF DB_ID('${MSSQL_DB}') IS NOT NULL
BEGIN
  ALTER DATABASE [${MSSQL_DB}] SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
  DROP DATABASE [${MSSQL_DB}];
END;
CREATE DATABASE [${MSSQL_DB}];
SQL

echo "[MSSQL] Wgrywam schemat i tabele..."
docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -d "$MSSQL_DB" -i /dev/stdin < schema/sql/create_schema.sql
docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -d "$MSSQL_DB" -i /dev/stdin < schema/sql/create_schema_structure.sql

echo "[MSSQL] Generuje dane bulk (${DATASET_ROWS})..."
python3 data_generator/generate_inserts.py -total-rows "$DATASET_ROWS" -engine mssql -relational-load-mode bulk -mssql-batch-size 2000 -mssql-rows-per-batch 2000

echo "[MSSQL] Kopiuje paczke bulk do kontenera..."
docker compose exec -T -u 0 mssql /bin/bash -lc 'rm -rf /var/opt/mssql/import && mkdir -p /var/opt/mssql/import && chown -R mssql:mssql /var/opt/mssql/import'
docker compose cp "generated_sql/bulk_mssql_${DATASET_ROWS}/." mssql:/var/opt/mssql/import
docker compose exec -T -u 0 mssql /bin/bash -lc 'chown -R mssql:mssql /var/opt/mssql/import'

echo "[MSSQL] Wgrywam dane przez BULK INSERT..."
docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -d "$MSSQL_DB" -i /var/opt/mssql/import/load_mssql_bulk_insert.sql

echo "[MSSQL] Kontrola liczby uzytkownikow..."
docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_PASSWORD" -C -d "$MSSQL_DB" -Q "SET NOCOUNT ON; SELECT COUNT(*) AS users_count FROM bench.BookshopUser;"

echo "[MSSQL] GOTOWE"
