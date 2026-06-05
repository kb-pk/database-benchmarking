#!/bin/bash

set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_SCRIPT="${ROOT_DIR}/run_benchmark.sh"

if [[ ! -x "$RUN_SCRIPT" ]]; then
  echo "Brak wykonywalnego skryptu: $RUN_SCRIPT"
  echo "Nadaj uprawnienia: chmod +x $RUN_SCRIPT"
  exit 1
fi

# Domyślnie uruchamiamy wszystkie 4 silniki po kolei.
if [[ "$#" -gt 0 ]]; then
  ENGINES=("$@")
else
  ENGINES=(POSTGRESQL MSSQL CASSANDRA SCYLLA)
fi

FAILED=0
declare -a SUMMARY

echo "Start benchmarków: $(date '+%Y-%m-%d %H:%M:%S')"
echo "Silniki: ${ENGINES[*]}"

prepare_resources_for_engine() {
  local engine="$1"

  case "$engine" in
    POSTGRESQL)
      docker compose stop mssql cassandra scylla >/dev/null 2>&1 || true
      docker compose up -d postgres backend >/dev/null
      ;;
    MSSQL)
      docker compose stop cassandra scylla >/dev/null 2>&1 || true
      docker compose up -d mssql postgres backend >/dev/null
      ;;
    CASSANDRA)
      docker compose stop mssql scylla >/dev/null 2>&1 || true
      docker compose up -d cassandra postgres backend >/dev/null
      ;;
    SCYLLA)
      docker compose stop mssql cassandra >/dev/null 2>&1 || true
      docker compose up -d scylla postgres backend >/dev/null
      ;;
  esac
}

for ENGINE in "${ENGINES[@]}"; do
  ENGINE_UPPER="${ENGINE^^}"
  echo
  echo "============================================================"
  echo "Uruchamiam benchmark dla: ${ENGINE_UPPER}"
  echo "============================================================"

  echo "Przygotowuję zasoby dla ${ENGINE_UPPER} (wyłączam nieużywane silniki)..."
  prepare_resources_for_engine "$ENGINE_UPPER"

  if "$RUN_SCRIPT" "$ENGINE_UPPER"; then
    SUMMARY+=("${ENGINE_UPPER}: OK")
  else
    CODE=$?
    SUMMARY+=("${ENGINE_UPPER}: FAIL (exit=${CODE})")
    FAILED=1
  fi
done

echo
echo "==================== PODSUMOWANIE ===================="
for ROW in "${SUMMARY[@]}"; do
  echo "$ROW"
done
echo "======================================================"

if [[ "$FAILED" -ne 0 ]]; then
  exit 1
fi

exit 0