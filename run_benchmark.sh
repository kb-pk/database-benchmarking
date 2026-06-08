#!/bin/bash

set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [[ -f "${SCRIPT_DIR}/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${SCRIPT_DIR}/.env"
  set +a
fi

DB_ENGINE="${1:-POSTGRESQL}"
DB_ENGINE="${DB_ENGINE^^}"

case "$DB_ENGINE" in
  POSTGRESQL|MSSQL|CASSANDRA|SCYLLA)
    ;;
  *)
    echo "Nieobsługiwany silnik: $DB_ENGINE. Użyj: POSTGRESQL, MSSQL, CASSANDRA albo SCYLLA"
    exit 1
    ;;
esac

BASE_URL="http://localhost:8080"
BENCH_WARMUP_ITERATIONS="${BENCH_WARMUP_ITERATIONS:-1}"
BENCH_MEASURED_ITERATIONS="${BENCH_MEASURED_ITERATIONS:-3}"
BENCH_TOTAL_ITERATIONS=$((BENCH_WARMUP_ITERATIONS + BENCH_MEASURED_ITERATIONS))
BENCH_ENABLE_EXPLAIN="${BENCH_ENABLE_EXPLAIN:-true}"
BENCH_EXPLAIN_OUTPUT_BASE_DIR="${BENCH_EXPLAIN_OUTPUT_BASE_DIR:-output_data/explain}"
MSSQL_SQLCMD_RETRIES="${MSSQL_SQLCMD_RETRIES:-5}"
MSSQL_SQLCMD_RETRY_DELAY_SECONDS="${MSSQL_SQLCMD_RETRY_DELAY_SECONDS:-2}"
MSSQL_SQLCMD_LOGIN_TIMEOUT_SECONDS="${MSSQL_SQLCMD_LOGIN_TIMEOUT_SECONDS:-60}"
if [[ "$DB_ENGINE" == "MSSQL" ]]; then
  BENCH_REQUEST_COOLDOWN_SECONDS="${BENCH_REQUEST_COOLDOWN_SECONDS:-0.20}"
else
  BENCH_REQUEST_COOLDOWN_SECONDS="${BENCH_REQUEST_COOLDOWN_SECONDS:-0}"
fi

MSSQL_EFFECTIVE_PASSWORD="${MSSQL_SA_PWD:-}"
if [[ -z "$MSSQL_EFFECTIVE_PASSWORD" ]]; then
  MSSQL_EFFECTIVE_PASSWORD=$(docker compose exec -T mssql /bin/bash -lc 'printf %s "$MSSQL_SA_PASSWORD"' 2>/dev/null || true)
fi

normalize_single_line_output() {
  printf '%s\n' "$1" \
    | tr -d '\r' \
    | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e '/^$/d' \
    | grep -vE '^\([0-9]+ rows affected\)$' \
    | head -n 1
}

normalize_multi_line_output() {
  printf '%s\n' "$1" \
    | tr -d '\r' \
    | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e '/^$/d' \
    | grep -vE '^\([0-9]+ rows affected\)$'
}

BENCH_EXPLAIN_ENABLED=0
if [[ "${BENCH_ENABLE_EXPLAIN,,}" == "true" || "$BENCH_ENABLE_EXPLAIN" == "1" ]]; then
  BENCH_EXPLAIN_ENABLED=1
fi

BENCH_EXPLAIN_ENGINE_DIR=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')
BENCH_EXPLAIN_OUTPUT_DIR="${BENCH_EXPLAIN_OUTPUT_BASE_DIR}/${BENCH_EXPLAIN_ENGINE_DIR}"
BENCH_EXPLAIN_UNSUPPORTED_REPORTED=0
if [[ "$BENCH_EXPLAIN_ENABLED" -eq 1 ]]; then
  mkdir -p "$BENCH_EXPLAIN_OUTPUT_DIR"
fi

extract_operation_from_label() {
  local label="$1"
  printf '%s\n' "$label" | awk '{print $1}'
}

extract_operation_from_url() {
  local url="$1"
  local operation

  operation=$(printf '%s\n' "$url" | sed -n 's/.*[?&]operation=\([^&]*\).*/\1/p' | head -n 1)
  if [[ -z "$operation" ]]; then
    operation=$(printf '%s\n' "$url" | sed -n 's/.*[?&]op=\([^&]*\).*/\1/p' | head -n 1)
  fi

  if [[ -n "$operation" ]]; then
    printf '%s\n' "$operation"
    return 0
  fi

  return 1
}

extract_explain_phase_from_label() {
  local label="$1"
  local lower

  lower=$(printf '%s\n' "$label" | tr '[:upper:]' '[:lower:]')
  if [[ "$lower" == *"restore"* ]]; then
    printf '%s\n' "restore"
    return 0
  fi
  if [[ "$lower" == *"cleanup"* ]]; then
    printf '%s\n' "cleanup"
    return 0
  fi
  if [[ "$lower" == *"setup"* ]]; then
    printf '%s\n' "setup"
    return 0
  fi

  printf '%s\n' "main"
}

extract_iteration_from_label() {
  local label="$1"
  if [[ "$label" =~ iter=([0-9]+) ]]; then
    printf '%s\n' "${BASH_REMATCH[1]}"
    return 0
  fi
  return 1
}

resolve_shop_id_from_url() {
  local url="$1"
  if [[ "$url" =~ /bookshop/([0-9a-fA-F-]+)/ ]]; then
    printf '%s\n' "${BASH_REMATCH[1]}"
    return 0
  fi

  if [[ "$url" =~ /active-by-shop/([0-9a-fA-F-]+) ]]; then
    printf '%s\n' "${BASH_REMATCH[1]}"
    return 0
  fi

  if [[ -n "${SHOP_ID:-}" ]]; then
    printf '%s\n' "$SHOP_ID"
    return 0
  fi

  return 1
}

build_explain_query_for_operation() {
  local operation="$1"
  local shop_id="$2"

  case "$DB_ENGINE" in
    POSTGRESQL)
      case "$operation" in
        R1)
          cat <<SQL
SELECT b.id, b.author, b.title, b.publisher, b.publishdate, b.pages, b.isinreadingroom
FROM bench.book b
WHERE b.bookshopid = ${shop_id}
ORDER BY b.id;
SQL
          ;;
        R2)
          cat <<SQL
SELECT u.id, u.name, u.surname, u.phonenumber, u.email, s.status
FROM bench.bookshopuser u
JOIN bench.activationstatus s ON s.id = u.isactiveid
WHERE u.mainbookshopid = ${shop_id}
  AND UPPER(s.status) = 'ACTIVE'
ORDER BY u.id;
SQL
          ;;
        R3)
          cat <<SQL
SELECT
  u.id,
  u.name,
  u.surname,
  COUNT(DISTINCT br.id) AS reservation_count,
  COUNT(DISTINCT rt.id) AS rental_count,
  COUNT(DISTINCT br.id) + COUNT(DISTINCT rt.id) AS total_count
FROM bench.bookshopuser u
LEFT JOIN bench.bookreservation br ON br.userid = u.id
LEFT JOIN bench.bookrental rt ON rt.userid = u.id
GROUP BY u.id, u.name, u.surname
HAVING COUNT(DISTINCT br.id) > 0 OR COUNT(DISTINCT rt.id) > 0
ORDER BY total_count DESC, u.id ASC;
SQL
          ;;
        R4)
          cat <<SQL
SELECT e.id, e.name, e.surname, COUNT(r.id) AS rental_count
FROM bench.bookrental r
JOIN bench.employee e ON e.id = r.employeeid
GROUP BY e.id, e.name, e.surname
ORDER BY rental_count DESC;
SQL
          ;;
        R5)
          cat <<SQL
WITH ranked_books AS (
  SELECT b.id AS book_id, b.title, b.author, COUNT(r.id) AS rental_count,
         RANK() OVER (ORDER BY COUNT(r.id) DESC) AS rank
  FROM bench.bookrental r
  JOIN bench.book b ON r.bookid = b.id
  WHERE b.bookshopid = ${shop_id}
  GROUP BY b.id, b.title, b.author
)
SELECT book_id, title, author, rental_count, rank
FROM ranked_books
ORDER BY rank, book_id;
SQL
          ;;
        R6)
          cat <<SQL
SELECT u.id, u.name, u.surname, u.phonenumber, u.email
FROM bench.bookshopuser u
WHERE EXISTS (
    SELECT 1
    FROM bench.bookreservation br
    WHERE br.userid = u.id
      AND br.whenreserved BETWEEN DATE '2000-01-01' AND DATE '2100-01-01'
)
AND EXISTS (
    SELECT 1
    FROM bench.bookrental rt
    WHERE rt.userid = u.id
      AND rt.startdate BETWEEN DATE '2000-01-01' AND DATE '2100-01-01'
)
ORDER BY u.id;
SQL
          ;;
        U1)
          cat <<SQL
UPDATE bench.useraccount
SET permissionsid = permissionsid
WHERE userid = (SELECT userid FROM bench.useraccount ORDER BY userid LIMIT 1);
SQL
          ;;
        U2)
          cat <<SQL
UPDATE bench.bookshopopeninghours oh
SET opensatmonday = oh.opensatmonday, closesatmonday = oh.closesatmonday
FROM bench.bookshop bs
WHERE bs.openinghoursid = oh.id
  AND bs.id = (SELECT id FROM bench.bookshop ORDER BY id LIMIT 1);
SQL
          ;;
        U3)
          cat <<SQL
UPDATE bench.bookshopuser u
SET isactiveid = u.isactiveid
WHERE u.id IN (
  SELECT u2.id
  FROM bench.bookshopuser u2
  JOIN bench.activationstatus s ON s.id = u2.isactiveid
  WHERE UPPER(COALESCE(s.status, '')) = 'ACTIVE'
  LIMIT 50
);
SQL
          ;;
        U4)
          cat <<SQL
UPDATE bench.employee
SET primarybookshopid = primarybookshopid
WHERE id = (SELECT id FROM bench.employee ORDER BY id LIMIT 1);
SQL
          ;;
        U5)
          cat <<SQL
UPDATE bench.bookrental
SET isreturned = COALESCE(isreturned, 0)
WHERE startdate < CURRENT_DATE - INTERVAL '30 days';
SQL
          ;;
        U6)
          cat <<SQL
UPDATE bench.bookshopuser
SET mainbookshopid = mainbookshopid
WHERE id IN (SELECT id FROM bench.bookshopuser ORDER BY id LIMIT 50);
SQL
          ;;
        C1)
          cat <<SQL
INSERT INTO bench.useraccountpermissions (id, permission, details)
VALUES ((SELECT COALESCE(MAX(id), 0) + 1 FROM bench.useraccountpermissions), 'EXPLAIN_C1', 'trace');
SQL
          ;;
        C2)
          cat <<SQL
SELECT id, shopname, address, email
FROM bench.bookshop
ORDER BY id DESC
LIMIT 20;
SQL
          ;;
        C3)
          cat <<SQL
SELECT id, name, surname, email
FROM bench.bookshopuser
ORDER BY id DESC
LIMIT 20;
SQL
          ;;
        C4|D1_SETUP)
          cat <<SQL
SELECT id, userid, bookid, whenreserved
FROM bench.bookreservation
ORDER BY id DESC
LIMIT 20;
SQL
          ;;
        C5|D2_SETUP|D5_SETUP|D6_SETUP)
          cat <<SQL
SELECT id, userid, bookid, bookshopid, startdate
FROM bench.bookrental
ORDER BY id DESC
LIMIT 20;
SQL
          ;;
        C6)
          cat <<SQL
SELECT id, title, author, bookshopid
FROM bench.book
ORDER BY id DESC
LIMIT 20;
SQL
          ;;
        D1)
          cat <<SQL
DELETE FROM bench.bookreservation
WHERE id = (SELECT id FROM bench.bookreservation ORDER BY id DESC LIMIT 1);
SQL
          ;;
        D2|D5_SETUP_CLEANUP|D6_SETUP_CLEANUP)
          cat <<SQL
DELETE FROM bench.bookrental
WHERE id = (SELECT id FROM bench.bookrental ORDER BY id DESC LIMIT 1);
SQL
          ;;
        D3)
          cat <<SQL
DELETE FROM bench.bookreservation
WHERE id IN (
  SELECT id FROM bench.bookreservation
  WHERE whenreserved < CURRENT_DATE - INTERVAL '2 months'
  LIMIT 50
);
SQL
          ;;
        D4)
          cat <<SQL
DELETE FROM bench.bookshopuser
WHERE id IN (SELECT id FROM bench.bookshopuser ORDER BY id DESC LIMIT 50);
SQL
          ;;
        D5)
          cat <<SQL
DELETE FROM bench.bookshopoffering
WHERE id IN (SELECT id FROM bench.bookshopoffering ORDER BY id DESC LIMIT 50);
SQL
          ;;
        D6)
          cat <<SQL
DELETE FROM bench.bookrental
WHERE id IN (SELECT id FROM bench.bookrental ORDER BY id DESC LIMIT 50);
SQL
          ;;
        *)
          return 1
          ;;
      esac
      ;;
    MSSQL)
      case "$operation" in
        R1)
          cat <<SQL
SELECT b.id, b.author, b.title, b.publisher, b.publishDate, b.pages, b.isInReadingRoom
FROM bench.Book b
WHERE b.bookShopId = ${shop_id}
ORDER BY b.id;
SQL
          ;;
        R2)
          cat <<SQL
SELECT u.id, u.name, u.surname, u.phoneNumber, u.email, s.status
FROM bench.BookShopUser u
JOIN bench.ActivationStatus s ON s.id = u.isActiveId
WHERE u.mainBookShopId = ${shop_id}
  AND UPPER(LTRIM(RTRIM(REPLACE(ISNULL(s.status, ''), CHAR(13), '')))) = 'ACTIVE'
ORDER BY u.id;
SQL
          ;;
        R3)
          cat <<SQL
SELECT
  u.id AS userId,
  u.name,
  u.surname,
  COALESCE(br.reservationCount, 0) AS reservationCount,
  COALESCE(rt.rentalCount, 0) AS rentalCount,
  COALESCE(br.reservationCount, 0) + COALESCE(rt.rentalCount, 0) AS totalCount
FROM bench.BookShopUser u
LEFT JOIN (
  SELECT userId, COUNT_BIG(*) AS reservationCount
  FROM bench.BookReservation
  GROUP BY userId
) br ON br.userId = u.id
LEFT JOIN (
  SELECT userId, COUNT_BIG(*) AS rentalCount
  FROM bench.BookRental
  GROUP BY userId
) rt ON rt.userId = u.id
WHERE COALESCE(br.reservationCount, 0) > 0 OR COALESCE(rt.rentalCount, 0) > 0
ORDER BY totalCount DESC, u.id ASC;
SQL
          ;;
        R4)
          cat <<SQL
SELECT e.id, e.name, e.surname, COUNT(r.id) AS rental_count
FROM bench.BookRental r
JOIN bench.Employee e ON e.id = r.employeeId
GROUP BY e.id, e.name, e.surname
ORDER BY rental_count DESC;
SQL
          ;;
        R5)
          cat <<SQL
WITH ranked_books AS (
  SELECT b.id AS book_id, b.title, b.author, COUNT(r.id) AS rental_count,
         RANK() OVER (ORDER BY COUNT(r.id) DESC) AS rank
  FROM bench.bookrental r
  JOIN bench.book b ON r.bookid = b.id
  WHERE b.bookshopid = ${shop_id}
  GROUP BY b.id, b.title, b.author
)
SELECT book_id, title, author, rental_count, rank
FROM ranked_books
ORDER BY rank, book_id;
SQL
          ;;
        R6)
          cat <<SQL
SELECT u.id, u.name, u.surname, u.phoneNumber, u.email
FROM bench.BookShopUser u
WHERE EXISTS (
    SELECT 1
    FROM bench.BookReservation br
    WHERE br.userId = u.id
      AND br.whenReserved BETWEEN '2000-01-01' AND '2100-01-01'
)
AND EXISTS (
    SELECT 1
    FROM bench.BookRental rt
    WHERE rt.userId = u.id
      AND rt.startDate BETWEEN '2000-01-01' AND '2100-01-01'
)
ORDER BY u.id;
SQL
          ;;
        U1)
          cat <<SQL
UPDATE bench.UserAccount
SET permissionsId = permissionsId
WHERE userId = (SELECT TOP 1 userId FROM bench.UserAccount ORDER BY userId);
SQL
          ;;
        U2)
          cat <<SQL
UPDATE oh
SET opensAtMonday = oh.opensAtMonday, closesAtMonday = oh.closesAtMonday
FROM bench.BookShopOpeningHours oh
JOIN bench.BookShop bs ON bs.openingHoursId = oh.id
WHERE bs.id = (SELECT TOP 1 id FROM bench.BookShop ORDER BY id);
SQL
          ;;
        U3)
          cat <<SQL
UPDATE bench.BookShopUser
SET isActiveId = isActiveId
WHERE id IN (SELECT TOP 50 id FROM bench.BookShopUser ORDER BY id);
SQL
          ;;
        U4)
          cat <<SQL
UPDATE bench.Employee
SET primaryBookShopId = primaryBookShopId
WHERE id = (SELECT TOP 1 id FROM bench.Employee ORDER BY id);
SQL
          ;;
        U5)
          cat <<SQL
UPDATE bench.BookRental
SET isReturned = ISNULL(isReturned, 0)
WHERE startDate < DATEADD(DAY, -30, CAST(GETDATE() AS date));
SQL
          ;;
        U6)
          cat <<SQL
UPDATE bench.BookShopUser
SET mainBookShopId = mainBookShopId
WHERE id IN (SELECT TOP 50 id FROM bench.BookShopUser ORDER BY id);
SQL
          ;;
        C1)
          cat <<SQL
INSERT INTO bench.UserAccountPermissions (id, permission, details)
VALUES ((SELECT ISNULL(MAX(id), 0) + 1 FROM bench.UserAccountPermissions), 'EXPLAIN_C1', 'trace');
SQL
          ;;
        C2)
          cat <<SQL
SELECT TOP 20 id, shopName, address, email
FROM bench.BookShop
ORDER BY id DESC;
SQL
          ;;
        C3)
          cat <<SQL
SELECT TOP 20 id, name, surname, email
FROM bench.BookShopUser
ORDER BY id DESC;
SQL
          ;;
        C4|D1_SETUP)
          cat <<SQL
SELECT TOP 20 id, userId, bookId, whenReserved
FROM bench.BookReservation
ORDER BY id DESC;
SQL
          ;;
        C5|D2_SETUP|D5_SETUP|D6_SETUP)
          cat <<SQL
SELECT TOP 20 id, userId, bookId, bookShopId, startDate
FROM bench.BookRental
ORDER BY id DESC;
SQL
          ;;
        C6)
          cat <<SQL
SELECT TOP 20 id, title, author, bookShopId
FROM bench.Book
ORDER BY id DESC;
SQL
          ;;
        D1)
          cat <<SQL
DELETE FROM bench.BookReservation
WHERE id = (SELECT TOP 1 id FROM bench.BookReservation ORDER BY id DESC);
SQL
          ;;
        D2|D5_SETUP_CLEANUP|D6_SETUP_CLEANUP)
          cat <<SQL
DELETE FROM bench.BookRental
WHERE id = (SELECT TOP 1 id FROM bench.BookRental ORDER BY id DESC);
SQL
          ;;
        D3)
          cat <<SQL
DELETE FROM bench.BookReservation
WHERE id IN (SELECT TOP 50 id FROM bench.BookReservation ORDER BY id DESC);
SQL
          ;;
        D4)
          cat <<SQL
DELETE FROM bench.BookShopUser
WHERE id IN (SELECT TOP 50 id FROM bench.BookShopUser ORDER BY id DESC);
SQL
          ;;
        D5)
          cat <<SQL
DELETE FROM bench.BookShopOffering
WHERE id IN (SELECT TOP 50 id FROM bench.BookShopOffering ORDER BY id DESC);
SQL
          ;;
        D6)
          cat <<SQL
DELETE FROM bench.BookRental
WHERE id IN (SELECT TOP 50 id FROM bench.BookRental ORDER BY id DESC);
SQL
          ;;
        *)
          return 1
          ;;
      esac
      ;;
    CASSANDRA|SCYLLA)
      case "$operation" in
        R1)
          cat <<SQL
SELECT book_id, title, author, publisher, publish_date, pages, is_in_reading_room
FROM books_by_shop
WHERE shop_id = ${shop_id}
LIMIT 20;
SQL
          ;;
        R2)
          cat <<SQL
SELECT user_id, name, surname, phone_number, email, status, main_book_shop_id
FROM users
WHERE main_book_shop_id = ${shop_id}
LIMIT 200
ALLOW FILTERING;
SQL
          ;;
        R3)
          cat <<SQL
SELECT user_id
FROM reservations_by_user
LIMIT 200;
SQL
          ;;
        R4)
          cat <<SQL
SELECT employee_id
FROM rentals_by_shop
LIMIT 200;
SQL
          ;;
        R5)
          cat <<SQL
SELECT book_id
FROM rentals_by_shop
WHERE shop_id = ${shop_id}
LIMIT 200;
SQL
          ;;
        R6)
          cat <<SQL
SELECT user_id, when_reserved
FROM reservations_by_user
LIMIT 200;
SQL
          ;;
        U1|C1)
          cat <<SQL
SELECT user_id, permissions
FROM users
LIMIT 200;
SQL
          ;;
        U2|C2)
          cat <<SQL
SELECT shop_id, opens_at_monday, closes_at_monday
FROM bookshops
LIMIT 200;
SQL
          ;;
        U3|D4|C3)
          cat <<SQL
SELECT user_id, status, main_book_shop_id
FROM users
LIMIT 200;
SQL
          ;;
        U4|D6)
          cat <<SQL
SELECT employee_id, primary_book_shop_id
FROM employees_by_shop
LIMIT 200;
SQL
          ;;
        U5|C5|D2|D5_SETUP_CLEANUP|D6_SETUP_CLEANUP)
          cat <<SQL
SELECT rental_id, user_id, shop_id, start_date, is_returned
FROM rentals_by_user
LIMIT 200;
SQL
          ;;
        U6)
          cat <<SQL
SELECT user_id, main_book_shop_id
FROM users
LIMIT 200;
SQL
          ;;
        C4|D1|D1_SETUP|D3)
          cat <<SQL
SELECT reservation_id, user_id, when_reserved
FROM reservations_by_user
LIMIT 200;
SQL
          ;;
        C6|D5)
          cat <<SQL
SELECT shop_id, book_id, title, author
FROM books_by_shop
LIMIT 200;
SQL
          ;;
        D2_SETUP|D5_SETUP|D6_SETUP)
          cat <<SQL
SELECT rental_id, user_id, start_date
FROM rentals_by_user
LIMIT 200;
SQL
          ;;
        *)
          return 1
          ;;
      esac
      ;;
    *)
      return 1
      ;;
  esac
}

run_explain_query_postgres() {
  local query="$1"
  docker compose exec -T postgres psql -U postgres -d bench -qAt -P pager=off -c "EXPLAIN ${query}" 2>/dev/null || true
}

run_explain_query_mssql() {
  local query="$1"
  local attempt
  local result
  local container_sql
  local script_content
  local script_b64

  if [[ -z "$MSSQL_EFFECTIVE_PASSWORD" ]]; then
    return 1
  fi

  for ((attempt=1; attempt<=MSSQL_SQLCMD_RETRIES; attempt++)); do
    container_sql="/var/opt/mssql/bench_showplan_${$}_${attempt}.sql"

    script_content=$(printf 'SET SHOWPLAN_TEXT ON\nGO\n%s\nGO\nSET SHOWPLAN_TEXT OFF\nGO\n' "$query")
    script_b64=$(printf '%s' "$script_content" | base64 -w 0)
    docker compose exec -T mssql /bin/bash -lc "printf '%s' '$script_b64' | base64 -d > '$container_sql'" >/dev/null 2>&1 || true

    result=$(docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd \
      -S localhost -C -U sa -P "$MSSQL_EFFECTIVE_PASSWORD" \
      -l "$MSSQL_SQLCMD_LOGIN_TIMEOUT_SECONDS" -t 60 \
      -d bench -h -1 -W -i "$container_sql" 2>/dev/null || true)
    docker compose exec -T mssql /bin/bash -lc "rm -f '$container_sql'" >/dev/null 2>&1 || true

    if [[ -n "$(printf '%s\n' "$result" | tr -d '\r' | sed '/^[[:space:]]*$/d')" ]]; then
      printf '%s\n' "$result"
      return 0
    fi

    sleep "$MSSQL_SQLCMD_RETRY_DELAY_SECONDS"
  done

  return 1
}

run_explain_query_cql() {
  local query="$1"
  local cql_container
  local cql_bin
  local cql_user
  local cql_pwd
  local result

  cql_container=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')

  if [[ "$DB_ENGINE" == "CASSANDRA" ]]; then
    cql_bin="/opt/cassandra/bin/cqlsh"
    cql_user="${CASSANDRA_USER:-cassandra}"
    cql_pwd="${CASSANDRA_PWD:-cassandra}"
  else
    cql_bin="/usr/bin/cqlsh"
    cql_user="${SCYLLA_USER:-cassandra}"
    cql_pwd="${SCYLLA_PWD:-cassandra}"
  fi

  result=$(docker compose exec -T "$cql_container" "$cql_bin" -u "$cql_user" -p "$cql_pwd" -e "TRACING ON; USE bench; ${query}; TRACING OFF;" 2>/dev/null || true)
  if [[ -n "$(printf '%s\n' "$result" | tr -d '\r' | sed '/^[[:space:]]*$/d')" ]]; then
    printf '%s\n' "$result"
    return 0
  fi

  result=$(docker compose exec -T "$cql_container" "$cql_bin" -e "TRACING ON; USE bench; ${query}; TRACING OFF;" 2>/dev/null || true)
  if [[ -n "$(printf '%s\n' "$result" | tr -d '\r' | sed '/^[[:space:]]*$/d')" ]]; then
    printf '%s\n' "$result"
    return 0
  fi

  return 1
}

emit_explain_for_request() {
  local url="$1"
  local label="$2"
  local method="$3"

  if [[ "$BENCH_EXPLAIN_ENABLED" -ne 1 ]]; then
    return 0
  fi

  local operation
  if extract_operation_from_url "$url" >/dev/null; then
    operation=$(extract_operation_from_url "$url")
  else
    operation=$(extract_operation_from_label "$label")
  fi

  local iteration
  iteration="na"
  if extract_iteration_from_label "$label" >/dev/null; then
    iteration=$(extract_iteration_from_label "$label")
  fi

  local shop_id
  shop_id=1
  if resolve_shop_id_from_url "$url" >/dev/null; then
    shop_id=$(resolve_shop_id_from_url "$url")
  fi

  local explain_query
  explain_query=$(build_explain_query_for_operation "$operation" "$shop_id") || {
    echo "EXPLAIN ${label}: brak zdefiniowanego mapowania dla operacji (${DB_ENGINE})."
    return 0
  }

  local explain_output
  case "$DB_ENGINE" in
    POSTGRESQL)
      explain_output=$(run_explain_query_postgres "$explain_query")
      ;;
    MSSQL)
      explain_output=$(run_explain_query_mssql "$explain_query")
      ;;
    CASSANDRA|SCYLLA)
      explain_output=$(run_explain_query_cql "$explain_query")
      ;;
    *)
      if [[ "$BENCH_EXPLAIN_UNSUPPORTED_REPORTED" -eq 0 ]]; then
        echo "EXPLAIN: pomijam dla ${DB_ENGINE} (brak obsługi)."
        BENCH_EXPLAIN_UNSUPPORTED_REPORTED=1
      fi
      return 0
      ;;
  esac

  if [[ -z "$(printf '%s\n' "$explain_output" | tr -d '\r' | sed '/^[[:space:]]*$/d')" ]]; then
    echo "EXPLAIN ${label}: nie udało się pobrać planu wykonania."
    return 0
  fi

  local explain_file
  local explain_phase
  explain_phase=$(extract_explain_phase_from_label "$label")
  local explain_signature
  explain_signature=$(printf '%s' "${method}|${url}|${label}" | cksum | awk '{print $1}')
  explain_file="${BENCH_EXPLAIN_OUTPUT_DIR}/${operation}_${explain_phase}_iter_${iteration}_${explain_signature}.txt"
  printf '%s\n' "$explain_output" > "$explain_file"

  echo "EXPLAIN ${label} -> ${explain_file}"
  printf '%s\n' "$explain_output"
}

wait_for_backend() {
  local probe_url="$BASE_URL/bookshop/1/books?db=POSTGRESQL&onlyAvailable=false"
  local retries=40

  for ((i=1; i<=retries; i++)); do
    local tmp_output
    tmp_output=$(mktemp)
    code=$(curl -q -sS -o "$tmp_output" -w "%{http_code}" "$probe_url" || true)
    rm -f "$tmp_output"
    if [[ "$code" == "200" ]]; then
      return 0
    fi
    echo "Czekam na backend... próba ${i}/${retries} (health status=${code:-000})"
    sleep 1
  done

  echo "Backend nie osiągnął statusu gotowości (HTTP 200 na endpoint testowym API)."
  return 1
}

wait_for_backend || exit 1

# Czyszczenie pliku CSV
clear_csv_tmp=$(mktemp)
curl -q -sS -o "$clear_csv_tmp" -w "clear-csv status=%{http_code}\n" -X POST "$BASE_URL/benchmark/clear-csv?db=${DB_ENGINE}"
rm -f "$clear_csv_tmp"

call_endpoint() {
  local url="$1"
  local label="$2"
  local method="${3:-GET}"
  local code
  local attempt
  local max_attempts
  local tmp_output

  max_attempts=1
  if [[ "$DB_ENGINE" == "MSSQL" && "$method" == "GET" ]]; then
    # Dla MSSQL GET-y potrafią zwrócić chwilowe 5xx przy presji pamięci.
    max_attempts=3
  fi

  for ((attempt=1; attempt<=max_attempts; attempt++)); do
    tmp_output=$(mktemp)
    code=$(curl -q -sS --retry 4 --retry-connrefused --retry-delay 1 -o "$tmp_output" -w "%{http_code}" -X "$method" "$url" || echo "000")
    rm -f "$tmp_output"
    if [[ "$code" =~ ^2[0-9][0-9]$ || "$code" =~ ^4[0-9][0-9]$ || "$method" != "GET" || "$attempt" -eq "$max_attempts" ]]; then
      break
    fi
    sleep "$MSSQL_SQLCMD_RETRY_DELAY_SECONDS"
  done

  echo "${label} status=${code}"

  emit_explain_for_request "$url" "$label" "$method"

  if [[ "$BENCH_REQUEST_COOLDOWN_SECONDS" != "0" ]]; then
    sleep "$BENCH_REQUEST_COOLDOWN_SECONDS"
  fi
}

call_endpoint_post() {
  local url="$1"
  local label="$2"
  call_endpoint "$url" "$label" "POST"
}

call_endpoint_post_without_timing() {
  local url="$1"
  local label="$2"
  call_endpoint_post "${url}&skipBenchmarkTiming=true" "$label"
}

run_for_iterations() {
  local method="$1"
  local label="$2"
  local url_template="$3"
  local i
  local url

  for ((i=1; i<=BENCH_TOTAL_ITERATIONS; i++)); do
    url="${url_template//\{i\}/$i}"
    call_endpoint "${url}&warmupIterations=${BENCH_WARMUP_ITERATIONS}" "${label} iter=${i}" "$method"
  done
}

run_delete_with_restore_iterations() {
  local label="$1"
  local delete_url_template="$2"
  local restore_url_template="$3"
  local i
  local delete_url
  local restore_url

  for ((i=1; i<=BENCH_TOTAL_ITERATIONS; i++)); do
    delete_url="${delete_url_template//\{i\}/$i}"
    restore_url="${restore_url_template//\{i\}/$i}"
    call_endpoint_post "${delete_url}&warmupIterations=${BENCH_WARMUP_ITERATIONS}" "${label} iter=${i}"
    call_endpoint_post_without_timing "${restore_url}&warmupIterations=${BENCH_WARMUP_ITERATIONS}" "${label} restore iter=${i}"
  done
}

# shopId zależy od typu silnika: SQL używa liczby, Cassandra/Scylla UUID.
if [[ "$DB_ENGINE" == "CASSANDRA" || "$DB_ENGINE" == "SCYLLA" ]]; then
  CQL_CONTAINER=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')
  SHOP_ID=$(docker compose exec -T "$CQL_CONTAINER" cqlsh -e "USE bench; SELECT shop_id FROM bookshops LIMIT 1;" \
    | grep -Eo '[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}' \
    | head -n 1)

  if [[ -z "$SHOP_ID" ]]; then
    echo "Nie udało się pobrać shopId dla ${DB_ENGINE}. Sprawdź, czy dane są załadowane."
    exit 1
  fi
else
  SHOP_ID=1
fi

# R1: Pobierz książki z wybranego sklepu (4 iteracje)
run_for_iterations "GET" "R1" "$BASE_URL/bookshop/${SHOP_ID}/books?db=${DB_ENGINE}&operation=R1&iteration={i}"

# R2: Pobierz aktywnych użytkowników dla sklepu (4 iteracje)
run_for_iterations "GET" "R2" "$BASE_URL/user/active-by-shop/${SHOP_ID}?db=${DB_ENGINE}&operation=R2&iteration={i}"

# R3: Lista użytkowników z łączną liczbą rezerwacji i wypożyczeń, globalnie ze wszystkich sklepów (4 iteracje)
run_for_iterations "GET" "R3" "$BASE_URL/user/activity-counts?db=${DB_ENGINE}&operation=R3&iteration={i}"

# R4: Obciążenie pracowników (ile wypożyczeń obsłużyli), globalnie ze wszystkich sklepów (4 iteracje)
run_for_iterations "GET" "R4" "$BASE_URL/bookshop/employee-rental-counts?db=${DB_ENGINE}&operation=R4&iteration={i}"

# R5: Ranking najczęściej wypożyczanych książek per sklep (4 iteracje)
run_for_iterations "GET" "R5" "$BASE_URL/bookshop/${SHOP_ID}/book-rental-ranking?db=${DB_ENGINE}&operation=R5&iteration={i}"

# R6: Użytkownicy "zaangażowani" (mieli i rezerwacje, i wypożyczenia) w okresie, globalnie ze wszystkich sklepów (4 iteracje)
run_for_iterations "GET" "R6" "$BASE_URL/user/engaged?db=${DB_ENGINE}&from=2000-01-01&to=2100-01-01&operation=R6&iteration={i}"

run_u3_bulk() {
  # U3: Zmiana statusu na INACTIVE dla wszystkich aktywnych użytkowników
  # bez aktywnego wypożyczenia i bez rezerwacji (4 iteracje)
  run_for_iterations "POST" "U3" "$BASE_URL/user/activation-status/inactive-if-no-open-items?db=${DB_ENGINE}&restoreAfterUpdate=false&operation=U3&iteration={i}"
}

run_sql_query_single_postgres() {
  local query="$1"
  docker compose exec -T postgres psql -U postgres -d bench -Atc "$query"
}

run_sql_query_single_mssql() {
  local query="$1"
  local attempt
  local result

  if [[ -z "$MSSQL_EFFECTIVE_PASSWORD" ]]; then
    return 1
  fi

  for ((attempt=1; attempt<=MSSQL_SQLCMD_RETRIES; attempt++)); do
    result=$(docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd \
      -S localhost -C -U sa -P "$MSSQL_EFFECTIVE_PASSWORD" \
      -l "$MSSQL_SQLCMD_LOGIN_TIMEOUT_SECONDS" -t 60 \
      -d bench -h -1 -W -Q "$query" 2>/dev/null || true)

    result=$(normalize_single_line_output "$result")

    if [[ -n "$result" ]]; then
      printf '%s\n' "$result"
      return 0
    fi

    sleep "$MSSQL_SQLCMD_RETRY_DELAY_SECONDS"
  done

  return 1
}

run_sql_query_many_mssql() {
  local query="$1"
  local attempt
  local result

  if [[ -z "$MSSQL_EFFECTIVE_PASSWORD" ]]; then
    return 1
  fi

  for ((attempt=1; attempt<=MSSQL_SQLCMD_RETRIES; attempt++)); do
    result=$(docker compose exec -T mssql /opt/mssql-tools18/bin/sqlcmd \
      -S localhost -C -U sa -P "$MSSQL_EFFECTIVE_PASSWORD" \
      -l "$MSSQL_SQLCMD_LOGIN_TIMEOUT_SECONDS" -t 60 \
      -d bench -h -1 -W -Q "$query" 2>/dev/null || true)

    result=$(normalize_multi_line_output "$result")

    if [[ -n "$result" ]]; then
      printf '%s\n' "$result"
      return 0
    fi

    sleep "$MSSQL_SQLCMD_RETRY_DELAY_SECONDS"
  done

  return 1
}

run_updates_sql() {
  local query_single="$1"
  local u1_user_query
  local u1_current_permission_query
  local u1_target_permission_query
  local u2_shop_query
  local u4_employee_query
  local u4_current_shop_query
  local u4_target_shop_query
  local u6_source_shop_query
  local u6_target_shop_query

  case "$DB_ENGINE" in
    POSTGRESQL)
      u1_user_query="SELECT userid FROM bench.useraccount ORDER BY userid LIMIT 1;"
      u1_current_permission_query="SELECT permissionsid FROM bench.useraccount WHERE userid=%s ORDER BY id LIMIT 1;"
      u1_target_permission_query="SELECT id FROM bench.useraccountpermissions WHERE id <> %s ORDER BY id LIMIT 1;"
      u2_shop_query="SELECT id FROM bench.bookshop ORDER BY id LIMIT 1;"
      u4_employee_query="SELECT id FROM bench.employee WHERE primarybookshopid IS NOT NULL ORDER BY id LIMIT 1;"
      u4_current_shop_query="SELECT primarybookshopid FROM bench.employee WHERE id=%s;"
      u4_target_shop_query="SELECT id FROM bench.bookshop WHERE id <> %s ORDER BY id LIMIT 1;"
      u6_source_shop_query="SELECT mainbookshopid FROM bench.bookshopuser WHERE mainbookshopid IS NOT NULL GROUP BY mainbookshopid ORDER BY COUNT(*) DESC, mainbookshopid LIMIT 1;"
      u6_target_shop_query="SELECT id FROM bench.bookshop WHERE id <> %s ORDER BY id LIMIT 1;"
      ;;
    MSSQL)
      u1_user_query="SELECT TOP 1 userId FROM bench.UserAccount ORDER BY userId;"
      u1_current_permission_query="SELECT TOP 1 permissionsId FROM bench.UserAccount WHERE userId=%s ORDER BY id;"
      u1_target_permission_query="SELECT TOP 1 id FROM bench.UserAccountPermissions WHERE id <> %s ORDER BY id;"
      u2_shop_query="SELECT TOP 1 id FROM bench.BookShop ORDER BY id;"
      u4_employee_query="SELECT TOP 1 id FROM bench.Employee WHERE primaryBookShopId IS NOT NULL ORDER BY id;"
      u4_current_shop_query="SELECT primaryBookShopId FROM bench.Employee WHERE id=%s;"
      u4_target_shop_query="SELECT TOP 1 id FROM bench.BookShop WHERE id <> %s ORDER BY id;"
      u6_source_shop_query="SELECT TOP 1 mainBookShopId FROM bench.BookShopUser WHERE mainBookShopId IS NOT NULL GROUP BY mainBookShopId ORDER BY COUNT(*) DESC, mainBookShopId;"
      u6_target_shop_query="SELECT TOP 1 id FROM bench.BookShop WHERE id <> %s ORDER BY id;"
      ;;
    *)
      echo "run_updates_sql użyto dla nieobsługiwanego silnika: $DB_ENGINE"
      exit 1
      ;;
  esac

  # U1: Aktualizacja uprawnień konta użytkownika
  U1_USER_ID=$($query_single "$u1_user_query")
  U1_CURRENT_PERMISSION_ID=$($query_single "$(printf "$u1_current_permission_query" "$U1_USER_ID")")
  U1_TARGET_PERMISSION_ID=$($query_single "$(printf "$u1_target_permission_query" "$U1_CURRENT_PERMISSION_ID")")
  if [[ -z "$U1_USER_ID" || -z "$U1_CURRENT_PERMISSION_ID" || -z "$U1_TARGET_PERMISSION_ID" ]]; then
    echo "U1 pominięte: nie udało się ustalić parametrów testowych (userId/permissionsId)."
    exit 1
  fi
  run_for_iterations "POST" "U1" "$BASE_URL/user/account-permissions?db=${DB_ENGINE}&userId=${U1_USER_ID}&permissionsId=${U1_TARGET_PERMISSION_ID}&restoreAfterUpdate=false&operation=U1&iteration={i}"

  # U2: Aktualizacja godzin otwarcia sklepu (poniedziałek)
  U2_SHOP_ID=$($query_single "$u2_shop_query")
  if [[ -z "$U2_SHOP_ID" ]]; then
    echo "U2 pominięte: nie udało się ustalić shopId."
    exit 1
  fi
  run_for_iterations "POST" "U2" "$BASE_URL/bookshop/${U2_SHOP_ID}/opening-hours?db=${DB_ENGINE}&opensAt=07:30:00&closesAt=21:15:00&restoreAfterUpdate=false&operation=U2&iteration={i}"

  # U3: Ustawienie statusu INACTIVE dla aktywnych użytkowników
  # bez aktywnego wypożyczenia i bez rezerwacji
  run_u3_bulk

  # U4: Przypisanie pracownika do innego sklepu
  U4_EMPLOYEE_ID=$($query_single "$u4_employee_query")
  U4_CURRENT_SHOP_ID=$($query_single "$(printf "$u4_current_shop_query" "$U4_EMPLOYEE_ID")")
  U4_TARGET_SHOP_ID=$($query_single "$(printf "$u4_target_shop_query" "$U4_CURRENT_SHOP_ID")")
  if [[ -z "$U4_EMPLOYEE_ID" || -z "$U4_CURRENT_SHOP_ID" || -z "$U4_TARGET_SHOP_ID" ]]; then
    echo "U4 pominięte: nie udało się ustalić parametrów testowych (employeeId/shopId)."
    exit 1
  fi
  run_for_iterations "POST" "U4" "$BASE_URL/employee/${U4_EMPLOYEE_ID}/primary-shop?db=${DB_ENGINE}&shopId=${U4_TARGET_SHOP_ID}&restoreAfterUpdate=false&operation=U4&iteration={i}"
  # U5: Zamknięcie wypożyczeń przeterminowanych powyżej progu dni
  run_for_iterations "POST" "U5" "$BASE_URL/bookshop/rentals/close-overdue?db=${DB_ENGINE}&daysThreshold=30&restoreAfterUpdate=false&operation=U5&iteration={i}"

  # U6: Przeniesienie grupy czytelników między sklepami
  U6_SOURCE_SHOP_ID=$($query_single "$u6_source_shop_query")
  U6_TARGET_SHOP_ID=$($query_single "$(printf "$u6_target_shop_query" "$U6_SOURCE_SHOP_ID")")
  if [[ -z "$U6_SOURCE_SHOP_ID" || -z "$U6_TARGET_SHOP_ID" ]]; then
    echo "U6 pominięte: nie udało się ustalić sourceShopId/targetShopId."
    exit 1
  fi
  run_for_iterations "POST" "U6" "$BASE_URL/user/transfer-group-to-shop?db=${DB_ENGINE}&sourceShopId=${U6_SOURCE_SHOP_ID}&targetShopId=${U6_TARGET_SHOP_ID}&maxUsers=50&restoreAfterUpdate=false&operation=U6&iteration={i}"
}

run_updates_cql() {
  local cql_container
  local uuid_re
  local u1_user_id
  local u2_shop_id
  local u4_employee_id
  local u4_current_shop_id
  local u4_target_shop_id
  local u6_source_shop_id
  local u6_target_shop_id

  cql_container=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')
  uuid_re='[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

  u1_user_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT user_id FROM users LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  u2_shop_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT shop_id FROM bookshops LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  u4_employee_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT employee_id FROM employees_by_shop LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  u4_current_shop_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT primary_book_shop_id FROM employees_by_shop WHERE employee_id = ${u4_employee_id} ALLOW FILTERING;" | grep -Eo "$uuid_re" | head -n 1)
  u4_target_shop_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT shop_id FROM bookshops LIMIT 2;" | grep -Eo "$uuid_re" | tail -n 1)
  u6_source_shop_id="$u2_shop_id"
  u6_target_shop_id="$u4_target_shop_id"

  if [[ -z "$u1_user_id" || -z "$u2_shop_id" || -z "$u4_employee_id" || -z "$u4_current_shop_id" || -z "$u4_target_shop_id" ]]; then
    echo "U1-U6 pominięte: nie udało się ustalić wymaganych identyfikatorów UUID dla ${DB_ENGINE}."
    exit 1
  fi

  # U1: Aktualizacja uprawnień konta użytkownika
  run_for_iterations "POST" "U1" "$BASE_URL/user/account-permissions?db=${DB_ENGINE}&userId=${u1_user_id}&permissionsId=1&restoreAfterUpdate=false&operation=U1&iteration={i}"

  # U2: Aktualizacja godzin otwarcia sklepu (poniedziałek)
  run_for_iterations "POST" "U2" "$BASE_URL/bookshop/${u2_shop_id}/opening-hours?db=${DB_ENGINE}&opensAt=07:30:00&closesAt=21:15:00&restoreAfterUpdate=false&operation=U2&iteration={i}"

  # U3: Ustawienie statusu INACTIVE dla aktywnych użytkowników
  # bez aktywnego wypożyczenia i bez rezerwacji
  run_u3_bulk

  # U4: Przypisanie pracownika do innego sklepu
  run_for_iterations "POST" "U4" "$BASE_URL/employee/${u4_employee_id}/primary-shop?db=${DB_ENGINE}&shopId=${u4_target_shop_id}&restoreAfterUpdate=false&operation=U4&iteration={i}"

  # U5: Zamknięcie wypożyczeń przeterminowanych powyżej progu dni
  run_for_iterations "POST" "U5" "$BASE_URL/bookshop/rentals/close-overdue?db=${DB_ENGINE}&daysThreshold=30&restoreAfterUpdate=false&operation=U5&iteration={i}"

  # U6: Przeniesienie grupy czytelników między sklepami
  run_for_iterations "POST" "U6" "$BASE_URL/user/transfer-group-to-shop?db=${DB_ENGINE}&sourceShopId=${u6_source_shop_id}&targetShopId=${u6_target_shop_id}&maxUsers=50&restoreAfterUpdate=false&operation=U6&iteration={i}"
}

run_creates_sql() {
  local query_single="$1"
  local manager_query
  local c4_book_query
  local c4_user_query
  local c5_shop_query
  local c5_book_query
  local c5_user_query
  local c6_shop_query

  case "$DB_ENGINE" in
    POSTGRESQL)
      manager_query="SELECT id FROM bench.employee ORDER BY id LIMIT 1;"
      c4_book_query="SELECT id FROM bench.book ORDER BY id LIMIT 1;"
      c4_user_query="SELECT id FROM bench.bookshopuser ORDER BY id LIMIT 1;"
      c5_shop_query="SELECT b.id FROM bench.bookshop b JOIN bench.employee e ON e.primarybookshopid = b.id ORDER BY b.id LIMIT 1;"
      c5_book_query="SELECT id FROM bench.book WHERE bookshopid = %s ORDER BY id LIMIT 1;"
      c5_user_query="SELECT u.id FROM bench.bookshopuser u JOIN bench.activationstatus s ON s.id = u.isactiveid WHERE UPPER(COALESCE(s.status, '')) = 'ACTIVE' ORDER BY u.id LIMIT 1;"
      c6_shop_query="SELECT id FROM bench.bookshop ORDER BY id LIMIT 1;"
      ;;
    MSSQL)
      manager_query="SELECT TOP 1 id FROM bench.Employee ORDER BY id;"
      c4_book_query="SELECT TOP 4 b.id FROM bench.Book b WHERE NOT EXISTS (SELECT 1 FROM bench.BookReservation r WHERE r.bookId = b.id) ORDER BY b.id;"
      c4_user_query="SELECT TOP 1 id FROM bench.BookShopUser ORDER BY id;"
      c5_shop_query="SELECT TOP 1 b.id FROM bench.BookShop b INNER JOIN bench.Employee e ON e.primaryBookShopId = b.id ORDER BY b.id;"
      c5_book_query="SELECT TOP 1 b.id FROM bench.Book b WHERE b.bookShopId = %s AND NOT EXISTS (SELECT 1 FROM bench.BookRental r WHERE r.bookId = b.id) ORDER BY b.id;"
      c5_user_query="SELECT TOP 1 u.id FROM bench.BookShopUser u JOIN bench.ActivationStatus s ON s.id = u.isActiveId WHERE UPPER(LTRIM(RTRIM(REPLACE(ISNULL(s.status, ''), CHAR(13), '')))) = 'ACTIVE' ORDER BY u.id;"
      c6_shop_query="SELECT TOP 1 id FROM bench.BookShop ORDER BY id;"
      ;;
    *)
      echo "run_creates_sql użyto dla nieobsługiwanego silnika: $DB_ENGINE"
      exit 1
      ;;
  esac

  # C1: Dodanie nowego uprawnienia konta w silnikach SQL
  for i in {1..4}; do
    local permission_name
    permission_name="C1_SQL_PERMISSION_${i}_$(date +%s)"
    call_endpoint_post "$BASE_URL/user/account-permissions/create?db=${DB_ENGINE}&permission=${permission_name}&details=created_by_benchmark&restoreAfterCreate=false&operation=C1&iteration=${i}" "C1 iter=${i}"
  done

  # C2: Dodanie nowego sklepu (manager = pierwszy pracownik)
  local manager_id
  manager_id=$($query_single "$manager_query")
  if [[ -z "$manager_id" ]]; then
    echo "C2 pominięte: nie udało się ustalić managerId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C2" "$BASE_URL/bookshop/create?db=${DB_ENGINE}&shopName=Przykladowy_Sklep_C2&address=Przykladowa%20ulica%201&email=przykladowy.sklep.c2@example.com&managerId=${manager_id}&restoreAfterCreate=false&operation=C2&iteration={i}"

  # C3: Rejestracja użytkownika (użytkownik + karta + konto)
  run_for_iterations "POST" "C3" "$BASE_URL/user/registration/create?db=${DB_ENGINE}&name=Jan&surname=Kowalski&phoneNumber=500600700&email=jan.kowalski.c3%40example.com&login=c3_uzytkownik&passwordHash=c3_przykladowy_hash&restoreAfterCreate=false&operation=C3&iteration={i}"

  # C4: Dodanie rezerwacji książki przez użytkownika (z kontrolą, że istnieją)
  if [[ "$DB_ENGINE" == "MSSQL" ]]; then
    local c4_user_id
    local c4_book_ids_raw
    local -a c4_book_ids
    c4_user_id=$($query_single "$c4_user_query")
    c4_book_ids_raw=$(run_sql_query_many_mssql "$c4_book_query" || true)
    mapfile -t c4_book_ids <<< "$c4_book_ids_raw"
    if [[ -z "$c4_user_id" || ${#c4_book_ids[@]} -lt 4 ]]; then
      echo "C4 pominięte: nie udało się ustalić bookId/userId dla ${DB_ENGINE}."
      exit 1
    fi

    for i in {1..4}; do
      local c4_book_id
      c4_book_id="${c4_book_ids[$((i - 1))]}"
      call_endpoint_post "$BASE_URL/bookshop/reservations/create?db=${DB_ENGINE}&bookId=${c4_book_id}&userId=${c4_user_id}&whenReserved=2024-01-15&restoreAfterCreate=false&operation=C4&iteration=${i}" "C4 iter=${i}"
    done
  else
    for i in {1..4}; do
      local c4_book_id
      local c4_user_id
      c4_book_id=$($query_single "$c4_book_query")
      c4_user_id=$($query_single "$c4_user_query")
      if [[ -z "$c4_book_id" || -z "$c4_user_id" ]]; then
        echo "C4 pominięte: nie udało się ustalić bookId/userId dla ${DB_ENGINE}."
        exit 1
      fi

      call_endpoint_post "$BASE_URL/bookshop/reservations/create?db=${DB_ENGINE}&bookId=${c4_book_id}&userId=${c4_user_id}&whenReserved=2024-01-15&restoreAfterCreate=false&operation=C4&iteration=${i}" "C4 iter=${i}"
    done
  fi

  # C5: Warunkowe utworzenie wypożyczenia (użytkownik aktywny + książka należy do sklepu)
  if [[ "$DB_ENGINE" == "MSSQL" ]]; then
    local c5_user_id
    local c5_pairs_raw
    local -a c5_pairs
    c5_user_id=$($query_single "$c5_user_query")
    c5_pairs_raw=$(run_sql_query_many_mssql "SELECT TOP 4 CAST(b.bookShopId AS varchar(20)) + '|' + CAST(b.id AS varchar(20)) FROM bench.Book b WHERE NOT EXISTS (SELECT 1 FROM bench.BookRental r WHERE r.bookId = b.id) ORDER BY b.id;" || true)
    mapfile -t c5_pairs <<< "$c5_pairs_raw"
    if [[ -z "$c5_user_id" || ${#c5_pairs[@]} -lt 4 ]]; then
      echo "C5 pominięte: nie udało się ustalić shopId/bookId/userId dla ${DB_ENGINE}."
      exit 1
    fi

    for i in {1..4}; do
      local c5_shop_id
      local c5_book_id
      IFS='|' read -r c5_shop_id c5_book_id <<< "${c5_pairs[$((i - 1))]}"
      call_endpoint_post "$BASE_URL/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${c5_shop_id}&bookId=${c5_book_id}&userId=${c5_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=C5&iteration=${i}" "C5 iter=${i}"
    done
  else
    for i in {1..4}; do
      local c5_shop_id
      local c5_book_id
      local c5_user_id
      c5_shop_id=$($query_single "$c5_shop_query")
      c5_book_id=$($query_single "$(printf "$c5_book_query" "$c5_shop_id")")
      c5_user_id=$($query_single "$c5_user_query")
      if [[ -z "$c5_shop_id" || -z "$c5_book_id" || -z "$c5_user_id" ]]; then
        echo "C5 pominięte: nie udało się ustalić shopId/bookId/userId dla ${DB_ENGINE}."
        exit 1
      fi

      call_endpoint_post "$BASE_URL/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${c5_shop_id}&bookId=${c5_book_id}&userId=${c5_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=C5&iteration=${i}" "C5 iter=${i}"
    done
  fi

  # C6: Nowa dostawa do sklepu (batch 20 książek + oferta)
  local c6_shop_id
  c6_shop_id=$($query_single "$c6_shop_query")
  if [[ -z "$c6_shop_id" ]]; then
    echo "C6 pominięte: nie udało się ustalić shopId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C6" "$BASE_URL/bookshop/${c6_shop_id}/delivery/new-batch?db=${DB_ENGINE}&batchSize=20&restoreAfterCreate=false&operation=C6&iteration={i}"
}

run_deletes_sql() {
  local query_single="$1"
  local fresh_book_query
  local fresh_user_query
  local fresh_shop_query
  local d5_shop_query
  local d5_book_query
  local d6_employee_query
  local d6_shop_by_employee_query
  local d6_book_query

  case "$DB_ENGINE" in
    POSTGRESQL)
      fresh_book_query="SELECT id FROM bench.book WHERE bookshopid = %s ORDER BY id DESC LIMIT 1;"
      fresh_user_query="SELECT u.id FROM bench.bookshopuser u JOIN bench.activationstatus s ON s.id = u.isactiveid WHERE UPPER(COALESCE(s.status, '')) = 'ACTIVE' ORDER BY u.id DESC LIMIT 1;"
      fresh_shop_query="SELECT b.id FROM bench.bookshop b JOIN bench.employee e ON e.primarybookshopid = b.id ORDER BY b.id DESC LIMIT 1;"
      d5_shop_query="SELECT o.bookshopid FROM bench.bookshopoffering o JOIN bench.employee e ON e.primarybookshopid = o.bookshopid JOIN bench.book b ON b.id = o.bookid AND b.bookshopid = o.bookshopid WHERE NOT EXISTS (SELECT 1 FROM bench.bookrental br WHERE br.userid = %s AND COALESCE(br.isreturned, 0) = 0 AND br.bookid = o.bookid AND br.bookshopid = o.bookshopid) ORDER BY o.id LIMIT 1;"
      d5_book_query="SELECT o.bookid FROM bench.bookshopoffering o JOIN bench.book b ON b.id = o.bookid AND b.bookshopid = o.bookshopid WHERE o.bookshopid = %s AND NOT EXISTS (SELECT 1 FROM bench.bookrental br WHERE br.userid = %s AND COALESCE(br.isreturned, 0) = 0 AND br.bookid = o.bookid AND br.bookshopid = o.bookshopid) ORDER BY o.id LIMIT 1;"
      d6_employee_query="SELECT e.id FROM bench.employee e WHERE e.primarybookshopid IS NOT NULL ORDER BY e.id LIMIT 1;"
      d6_shop_by_employee_query="SELECT e.primarybookshopid FROM bench.employee e WHERE e.id = %s;"
      d6_book_query="SELECT b.id FROM bench.book b WHERE b.bookshopid = %s ORDER BY b.id LIMIT 1;"
      ;;
    MSSQL)
      fresh_book_query="SELECT TOP 1 b.id FROM bench.Book b WHERE b.bookShopId = %s AND NOT EXISTS (SELECT 1 FROM bench.BookReservation br WHERE br.bookId = b.id) AND NOT EXISTS (SELECT 1 FROM bench.BookRental r WHERE r.bookId = b.id) ORDER BY b.id DESC;"
      fresh_user_query="SELECT TOP 1 u.id FROM bench.BookShopUser u JOIN bench.ActivationStatus s ON s.id = u.isActiveId WHERE UPPER(LTRIM(RTRIM(REPLACE(ISNULL(s.status, ''), CHAR(13), '')))) = 'ACTIVE' ORDER BY u.id DESC;"
      fresh_shop_query="SELECT TOP 1 b.id FROM bench.BookShop b JOIN bench.Employee e ON e.primaryBookShopId = b.id ORDER BY b.id DESC;"
      d5_shop_query="SELECT TOP 1 o.bookShopId FROM bench.BookShopOffering o JOIN bench.Employee e ON e.primaryBookShopId = o.bookShopId JOIN bench.Book b ON b.id = o.bookId AND b.bookShopId = o.bookShopId WHERE NOT EXISTS (SELECT 1 FROM bench.BookRental br WHERE br.userId = %s AND ISNULL(br.isReturned, 0) = 0 AND br.bookId = o.bookId AND br.bookShopId = o.bookShopId) ORDER BY o.id;"
      d5_book_query="SELECT TOP 1 o.bookId FROM bench.BookShopOffering o JOIN bench.Book b ON b.id = o.bookId AND b.bookShopId = o.bookShopId WHERE o.bookShopId = %s AND NOT EXISTS (SELECT 1 FROM bench.BookRental br WHERE br.bookId = o.bookId) ORDER BY o.id;"
      d6_employee_query="SELECT TOP 1 e.id FROM bench.Employee e WHERE e.primaryBookShopId IS NOT NULL ORDER BY e.id;"
      d6_shop_by_employee_query="SELECT e.primaryBookShopId FROM bench.Employee e WHERE e.id = %s;"
      d6_book_query="SELECT TOP 1 b.id FROM bench.Book b WHERE b.bookShopId = %s AND NOT EXISTS (SELECT 1 FROM bench.BookRental r WHERE r.bookId = b.id) ORDER BY b.id;"
      ;;
    *)
      echo "run_deletes_sql użyto dla nieobsługiwanego silnika: $DB_ENGINE"
      exit 1
      ;;
  esac

  local fresh_shop_id
  fresh_shop_id=$($query_single "$fresh_shop_query")
  if [[ -z "$fresh_shop_id" ]]; then
    echo "D2 pominięte: nie udało się ustalić freshShopId dla ${DB_ENGINE}."
    exit 1
  fi

  local fresh_book_id
  local fresh_user_id
  fresh_book_id=$($query_single "$(printf "$fresh_book_query" "$fresh_shop_id")")
  fresh_user_id=$($query_single "$fresh_user_query")
  if [[ -z "$fresh_book_id" || -z "$fresh_user_id" ]]; then
    echo "D1 pominięte: nie udało się ustalić freshBookId/freshUserId dla ${DB_ENGINE}."
    exit 1
  fi

  local fresh_reservation_url
  fresh_reservation_url="http://localhost:8080/bookshop/reservations/create?db=${DB_ENGINE}&bookId=${fresh_book_id}&userId=${fresh_user_id}&whenReserved=2024-01-15&restoreAfterCreate=false&operation=D1_SETUP&iteration=1&skipBenchmarkTiming=true"
  local fresh_reservation_response
  local fresh_reservation_http_code
  local fresh_reservation_id
  fresh_reservation_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "$fresh_reservation_url")
  fresh_reservation_http_code=$(printf '%s\n' "$fresh_reservation_response" | tail -n 1)
  fresh_reservation_id=$(printf '%s\n' "$fresh_reservation_response" | sed '$d' | grep -oE '"createdReservationId":[0-9]+' | head -n 1 | cut -d: -f2)
  if [[ "$fresh_reservation_http_code" != "200" || -z "$fresh_reservation_id" ]]; then
    echo "D1 setup pominięte: nie udało się utworzyć świeżej rezerwacji dla ${DB_ENGINE}."
    exit 1
  fi

  local fresh_rental_query
  local fresh_rental_response
  local fresh_rental_http_code
  local fresh_rental_id
  fresh_rental_query="http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${fresh_shop_id}&bookId=${fresh_book_id}&userId=${fresh_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=D2_SETUP&iteration=1&skipBenchmarkTiming=true"
  fresh_rental_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "$fresh_rental_query")
  fresh_rental_http_code=$(printf '%s\n' "$fresh_rental_response" | tail -n 1)
  fresh_rental_id=$(printf '%s\n' "$fresh_rental_response" | sed '$d' | grep -oE '"createdRentalId":[0-9]+' | head -n 1 | cut -d: -f2)
  if [[ "$fresh_rental_http_code" != "200" || -z "$fresh_rental_id" ]]; then
    echo "D2 setup pominięte: nie udało się utworzyć świeżego wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  local reservation_id
  reservation_id="$fresh_reservation_id"
  if [[ -z "$reservation_id" ]]; then
    echo "D1 pominięte: nie udało się ustalić reservationId dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D1" \
    "$BASE_URL/bookshop/reservations/delete?db=${DB_ENGINE}&reservationId=${reservation_id}&restoreAfterDelete=false&operation=D1&iteration={i}" \
    "$BASE_URL/bookshop/reservations/delete?db=${DB_ENGINE}&reservationId=${reservation_id}&restoreAfterDelete=true&operation=D1&iteration={i}"

  run_delete_with_restore_iterations "D2" \
    "$BASE_URL/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${fresh_rental_id}&restoreAfterDelete=false&operation=D2&iteration={i}" \
    "$BASE_URL/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${fresh_rental_id}&restoreAfterDelete=true&operation=D2&iteration={i}"

  run_delete_with_restore_iterations "D3" \
    "$BASE_URL/bookshop/reservations/cleanup-old-unfinalized?db=${DB_ENGINE}&monthsThreshold=2&restoreAfterDelete=false&operation=D3&iteration={i}" \
    "$BASE_URL/bookshop/reservations/cleanup-old-unfinalized?db=${DB_ENGINE}&monthsThreshold=2&restoreAfterDelete=true&operation=D3&iteration={i}"

  run_delete_with_restore_iterations "D4" \
    "$BASE_URL/user/inactive-segment-delete?db=${DB_ENGINE}&monthsThreshold=3&segmentSize=50&restoreAfterDelete=false&operation=D4&iteration={i}" \
    "$BASE_URL/user/inactive-segment-delete?db=${DB_ENGINE}&monthsThreshold=3&segmentSize=50&restoreAfterDelete=true&operation=D4&iteration={i}"

  local d5_shop_id
  local d5_book_id
  local d5_user_id
  d5_user_id=$fresh_user_id
  d5_shop_id=$($query_single "$(printf "$d5_shop_query" "$d5_user_id")")
  d5_book_id=$($query_single "$(printf "$d5_book_query" "$d5_shop_id" "$d5_user_id")")
  if [[ -z "$d5_shop_id" || -z "$d5_book_id" || -z "$d5_user_id" ]]; then
    echo "D5 pominięte: nie udało się ustalić shopId/bookId/userId dla ${DB_ENGINE}."
    exit 1
  fi

  local d5_setup_response
  local d5_setup_http_code
  local d5_setup_rental_id
  d5_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${d5_shop_id}&bookId=${d5_book_id}&userId=${d5_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=D5_SETUP&iteration=1&skipBenchmarkTiming=true")
  d5_setup_http_code=$(printf '%s\n' "$d5_setup_response" | tail -n 1)
  d5_setup_rental_id=$(printf '%s\n' "$d5_setup_response" | sed '$d' | grep -oE '"createdRentalId":[0-9]+' | head -n 1 | cut -d: -f2)
  if [[ "$d5_setup_http_code" != "200" || -z "$d5_setup_rental_id" ]]; then
    echo "D5 setup pominięte: nie udało się utworzyć aktywnego wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D5" \
    "$BASE_URL/bookshop/offerings/delete-permanently-borrowed-by-user?db=${DB_ENGINE}&userId=${d5_user_id}&restoreAfterDelete=false&operation=D5&iteration={i}" \
    "$BASE_URL/bookshop/offerings/delete-permanently-borrowed-by-user?db=${DB_ENGINE}&userId=${d5_user_id}&restoreAfterDelete=true&operation=D5&iteration={i}"

  call_endpoint_post_without_timing "http://localhost:8080/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d5_setup_rental_id}&restoreAfterDelete=false&operation=D5_SETUP_CLEANUP&iteration=1" "D5 cleanup rental"

  local d6_employee_id
  local d6_shop_id
  local d6_book_id
  local d6_user_id
  local d6_rental_day
  d6_rental_day="2035-01-15"
  d6_employee_id=$($query_single "$d6_employee_query")
  d6_shop_id=$($query_single "$(printf "$d6_shop_by_employee_query" "$d6_employee_id")")
  d6_book_id=$($query_single "$(printf "$d6_book_query" "$d6_shop_id")")
  d6_user_id=$fresh_user_id
  if [[ -z "$d6_employee_id" || -z "$d6_shop_id" || -z "$d6_book_id" || -z "$d6_user_id" ]]; then
    echo "D6 pominięte: nie udało się ustalić employeeId/shopId/bookId/userId dla ${DB_ENGINE}."
    exit 1
  fi

  local d6_setup_response
  local d6_setup_http_code
  local d6_setup_rental_id
  d6_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${d6_shop_id}&bookId=${d6_book_id}&userId=${d6_user_id}&startDate=${d6_rental_day}&restoreAfterCreate=false&operation=D6_SETUP&iteration=1&skipBenchmarkTiming=true")
  d6_setup_http_code=$(printf '%s\n' "$d6_setup_response" | tail -n 1)
  d6_setup_rental_id=$(printf '%s\n' "$d6_setup_response" | sed '$d' | grep -oE '"createdRentalId":[0-9]+' | head -n 1 | cut -d: -f2)
  if [[ "$d6_setup_http_code" != "200" || -z "$d6_setup_rental_id" ]]; then
    echo "D6 setup pominięte: nie udało się utworzyć wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D6" \
    "$BASE_URL/bookshop/rentals/delete-by-employee-day?db=${DB_ENGINE}&employeeId=${d6_employee_id}&rentalDate=${d6_rental_day}&restoreAfterDelete=false&operation=D6&iteration={i}" \
    "$BASE_URL/bookshop/rentals/delete-by-employee-day?db=${DB_ENGINE}&employeeId=${d6_employee_id}&rentalDate=${d6_rental_day}&restoreAfterDelete=true&operation=D6&iteration={i}"

  call_endpoint_post_without_timing "http://localhost:8080/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d6_setup_rental_id}&restoreAfterDelete=false&operation=D6_SETUP_CLEANUP&iteration=1" "D6 cleanup rental"
}

run_creates_cql() {
  local cql_container
  local uuid_re
  local manager_id
  local c4_shop_id
  local c4_user_id
  local c4_book_id
  local c5_shop_id
  local c5_book_id
  local c5_user_id
  local c6_shop_id

  cql_container=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')
  uuid_re='[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

  # C1: Dodanie nowego uprawnienia konta do aktywnego użytkownika w silnikach CQL
  for i in {1..4}; do
    local permission_name
    permission_name="C1_CQL_PERMISSION_${i}_$(date +%s)"
    call_endpoint_post "$BASE_URL/user/account-permissions/create?db=${DB_ENGINE}&permission=${permission_name}&details=created_by_benchmark&restoreAfterCreate=false&operation=C1&iteration=${i}" "C1 iter=${i}"
  done

  # C2: Dodanie nowego sklepu (manager = pierwszy pracownik z CQL)
  manager_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT manager_id FROM bookshops LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)

  if [[ -z "$manager_id" ]]; then
    echo "C2 pominięte: nie udało się ustalić managerId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C2" "$BASE_URL/bookshop/create?db=${DB_ENGINE}&shopName=Przykladowy_Sklep_C2&address=Przykladowa%20ulica%201&email=przykladowy.sklep.c2@example.com&managerId=${manager_id}&restoreAfterCreate=false&operation=C2&iteration={i}"

  # C3: Rejestracja użytkownika (użytkownik + karta + konto)
  run_for_iterations "POST" "C3" "$BASE_URL/user/registration/create?db=${DB_ENGINE}&name=Jan&surname=Kowalski&phoneNumber=500600700&email=jan.kowalski.c3.cql%40example.com&login=c3_cql_uzytkownik_{i}&passwordHash=c3_przykladowy_hash&restoreAfterCreate=false&operation=C3&iteration={i}"

  # C4: Dodanie rezerwacji książki przez użytkownika (z kontrolą, że istnieją)
  c4_shop_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT shop_id FROM books_by_shop LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  c4_user_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT user_id FROM users WHERE status = 'ACTIVE' LIMIT 1 ALLOW FILTERING;" | grep -Eo "$uuid_re" | head -n 1)
  c4_book_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT book_id FROM books_by_shop WHERE shop_id = ${c4_shop_id} LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  if [[ -z "$c4_shop_id" || -z "$c4_user_id" || -z "$c4_book_id" ]]; then
    echo "C4 pominięte: nie udało się ustalić shopId/bookId/userId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C4" "$BASE_URL/bookshop/reservations/create?db=${DB_ENGINE}&bookId=${c4_book_id}&userId=${c4_user_id}&whenReserved=2024-01-15&restoreAfterCreate=false&operation=C4&iteration={i}"

  # C5: Warunkowe utworzenie wypożyczenia (użytkownik aktywny + książka należy do sklepu)
  c5_user_id="$c4_user_id"
  c5_book_id="$c4_book_id"
  c5_shop_id="$c4_shop_id"
  if [[ -z "$c5_shop_id" || -z "$c5_book_id" || -z "$c5_user_id" ]]; then
    echo "C5 pominięte: nie udało się ustalić shopId/bookId/userId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C5" "$BASE_URL/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${c5_shop_id}&bookId=${c5_book_id}&userId=${c5_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=C5&iteration={i}"

  # C6: Nowa dostawa do sklepu (batch 20 książek + oferta)
  c6_shop_id="$c5_shop_id"
  if [[ -z "$c6_shop_id" ]]; then
    echo "C6 pominięte: nie udało się ustalić shopId dla ${DB_ENGINE}."
    exit 1
  fi

  run_for_iterations "POST" "C6" "$BASE_URL/bookshop/${c6_shop_id}/delivery/new-batch?db=${DB_ENGINE}&batchSize=20&restoreAfterCreate=false&operation=C6&iteration={i}"
}

run_deletes_cql() {
  local cql_container
  local uuid_re
  local d_shop_id
  local d_book_id
  local d_user_id
  local d_employee_id
  local d6_rental_day

  cql_container=$(echo "$DB_ENGINE" | tr '[:upper:]' '[:lower:]')
  uuid_re='[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}'

  d_shop_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT primary_book_shop_id FROM employees_by_shop LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  d_book_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT book_id FROM books_by_shop WHERE shop_id = ${d_shop_id} LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  d_user_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT user_id FROM users WHERE status = 'ACTIVE' LIMIT 1 ALLOW FILTERING;" | grep -Eo "$uuid_re" | head -n 1)
  d_employee_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT employee_id FROM employees_by_shop WHERE primary_book_shop_id = ${d_shop_id} LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)

  if [[ -z "$d_shop_id" || -z "$d_book_id" || -z "$d_user_id" || -z "$d_employee_id" ]]; then
    echo "D1-D6 pominięte: nie udało się ustalić shopId/bookId/userId/employeeId dla ${DB_ENGINE}."
    exit 1
  fi

  local d1_setup_response
  local d1_setup_http_code
  local d1_reservation_id
  d1_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/reservations/create?db=${DB_ENGINE}&bookId=${d_book_id}&userId=${d_user_id}&whenReserved=2024-01-15&restoreAfterCreate=false&operation=D1_SETUP&iteration=1&skipBenchmarkTiming=true")
  d1_setup_http_code=$(printf '%s\n' "$d1_setup_response" | tail -n 1)
  d1_reservation_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT reservation_id FROM reservations_by_user WHERE user_id = ${d_user_id} AND when_reserved = '2024-01-15' LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  if [[ "$d1_setup_http_code" != "200" || -z "$d1_reservation_id" ]]; then
    echo "D1 setup pominięte: nie udało się utworzyć rezerwacji dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D1" \
    "$BASE_URL/bookshop/reservations/delete?db=${DB_ENGINE}&reservationId=${d1_reservation_id}&restoreAfterDelete=false&operation=D1&iteration={i}" \
    "$BASE_URL/bookshop/reservations/delete?db=${DB_ENGINE}&reservationId=${d1_reservation_id}&restoreAfterDelete=true&operation=D1&iteration={i}"

  local d2_setup_response
  local d2_setup_http_code
  local d2_rental_id
  d2_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${d_shop_id}&bookId=${d_book_id}&userId=${d_user_id}&startDate=2024-01-20&restoreAfterCreate=false&operation=D2_SETUP&iteration=1&skipBenchmarkTiming=true")
  d2_setup_http_code=$(printf '%s\n' "$d2_setup_response" | tail -n 1)
  d2_rental_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT rental_id FROM rentals_by_user WHERE user_id = ${d_user_id} AND start_date = '2024-01-20' LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  if [[ "$d2_setup_http_code" != "200" || -z "$d2_rental_id" ]]; then
    echo "D2 setup pominięte: nie udało się utworzyć wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D2" \
    "$BASE_URL/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d2_rental_id}&restoreAfterDelete=false&operation=D2&iteration={i}" \
    "$BASE_URL/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d2_rental_id}&restoreAfterDelete=true&operation=D2&iteration={i}"

  run_delete_with_restore_iterations "D3" \
    "$BASE_URL/bookshop/reservations/cleanup-old-unfinalized?db=${DB_ENGINE}&monthsThreshold=2&restoreAfterDelete=false&operation=D3&iteration={i}" \
    "$BASE_URL/bookshop/reservations/cleanup-old-unfinalized?db=${DB_ENGINE}&monthsThreshold=2&restoreAfterDelete=true&operation=D3&iteration={i}"

  run_delete_with_restore_iterations "D4" \
    "$BASE_URL/user/inactive-segment-delete?db=${DB_ENGINE}&monthsThreshold=3&segmentSize=50&restoreAfterDelete=false&operation=D4&iteration={i}" \
    "$BASE_URL/user/inactive-segment-delete?db=${DB_ENGINE}&monthsThreshold=3&segmentSize=50&restoreAfterDelete=true&operation=D4&iteration={i}"

  local d5_setup_response
  local d5_setup_http_code
  local d5_setup_rental_id
  d5_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${d_shop_id}&bookId=${d_book_id}&userId=${d_user_id}&startDate=2024-01-21&restoreAfterCreate=false&operation=D5_SETUP&iteration=1&skipBenchmarkTiming=true")
  d5_setup_http_code=$(printf '%s\n' "$d5_setup_response" | tail -n 1)
  d5_setup_rental_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT rental_id FROM rentals_by_user WHERE user_id = ${d_user_id} AND start_date = '2024-01-21' LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  if [[ "$d5_setup_http_code" != "200" || -z "$d5_setup_rental_id" ]]; then
    echo "D5 setup pominięte: nie udało się utworzyć wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D5" \
    "$BASE_URL/bookshop/offerings/delete-permanently-borrowed-by-user?db=${DB_ENGINE}&userId=${d_user_id}&restoreAfterDelete=false&operation=D5&iteration={i}" \
    "$BASE_URL/bookshop/offerings/delete-permanently-borrowed-by-user?db=${DB_ENGINE}&userId=${d_user_id}&restoreAfterDelete=true&operation=D5&iteration={i}"

  call_endpoint_post_without_timing "http://localhost:8080/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d5_setup_rental_id}&restoreAfterDelete=false&operation=D5_SETUP_CLEANUP&iteration=1" "D5 cleanup rental"

  d6_rental_day="2035-01-15"
  local d6_setup_response
  local d6_setup_http_code
  local d6_setup_rental_id
  d6_setup_response=$(curl -q -sS -X POST -w $'\n%{http_code}' "http://localhost:8080/bookshop/rentals/create-conditional?db=${DB_ENGINE}&shopId=${d_shop_id}&bookId=${d_book_id}&userId=${d_user_id}&startDate=${d6_rental_day}&restoreAfterCreate=false&operation=D6_SETUP&iteration=1&skipBenchmarkTiming=true")
  d6_setup_http_code=$(printf '%s\n' "$d6_setup_response" | tail -n 1)
  d6_setup_rental_id=$(docker compose exec -T "$cql_container" cqlsh -e "USE bench; SELECT rental_id FROM rentals_by_user WHERE user_id = ${d_user_id} AND start_date = '${d6_rental_day}' LIMIT 1;" | grep -Eo "$uuid_re" | head -n 1)
  if [[ "$d6_setup_http_code" != "200" || -z "$d6_setup_rental_id" ]]; then
    echo "D6 setup pominięte: nie udało się utworzyć wypożyczenia dla ${DB_ENGINE}."
    exit 1
  fi

  run_delete_with_restore_iterations "D6" \
    "$BASE_URL/bookshop/rentals/delete-by-employee-day?db=${DB_ENGINE}&employeeId=${d_employee_id}&rentalDate=${d6_rental_day}&restoreAfterDelete=false&operation=D6&iteration={i}" \
    "$BASE_URL/bookshop/rentals/delete-by-employee-day?db=${DB_ENGINE}&employeeId=${d_employee_id}&rentalDate=${d6_rental_day}&restoreAfterDelete=true&operation=D6&iteration={i}"

  call_endpoint_post_without_timing "http://localhost:8080/bookshop/rentals/delete?db=${DB_ENGINE}&rentalId=${d6_setup_rental_id}&restoreAfterDelete=false&operation=D6_SETUP_CLEANUP&iteration=1" "D6 cleanup rental"
}

# Update w stałej kolejności U1 -> U6
case "$DB_ENGINE" in
  POSTGRESQL)
    run_updates_sql run_sql_query_single_postgres
    run_creates_sql run_sql_query_single_postgres
    run_deletes_sql run_sql_query_single_postgres
    ;;
  MSSQL)
    run_updates_sql run_sql_query_single_mssql
    run_creates_sql run_sql_query_single_mssql
    run_deletes_sql run_sql_query_single_mssql
    ;;
  CASSANDRA|SCYLLA)
    run_updates_cql
    run_creates_cql
    run_deletes_cql
    ;;
esac