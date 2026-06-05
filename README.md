Link do UML: https://lucid.app/lucidchart/87988d5e-6ca7-449b-be75-491117a453e3/edit?viewport_loc=351%2C48%2C2175%2C1087%2C0_0&invitationId=inv_b0529357-1922-4cad-b745-aa6f6c0f57ab 

## Projekt w skrocie
- Benchmark jest wykonywany przez endpointy HTTP backendu
- Wyniki czasu operacji CRUD sa zapisywane do CSV w `output_data/<silnik>/crud_timings_<silnik>.csv`.
- Plik glowny CSV zawiera zestawienie czasow dla kolejnych iteracji mierzonych w formacie `operation,iteration_1_ms,iteration_2_ms,iteration_3_ms`.
- Surowe przebiegi pojedynczych iteracji sa zapisywane osobno w `output_data/<silnik>/crud_timings_<silnik>_raw.csv`.
- Kazdy test ma warmup; domyslnie skrypt wykonuje `1` iteracje warmup i `3` iteracje mierzone, a warmup nie wchodzi do pliku glownego CSV.
- Do automatycznego uruchomienia benchmarku sluzy skrypt `run_benchmark.sh`.

## Uruchamianie
`docker compose up --build -d`

Wymagany procesor ze wsparciem dla SSE4.2 (flaga `sse4_2`) oraz PCLMUL (flaga `pclmulqdq`). 
Pierwsze uruchomienie będzie nieco wolniejsze (zapełnienie lokalnego repozytorium Maven).

## Wymagane zmienne środowiskowe do uruchomienia docker compose

Przed uruchomieniem kontenerów należy ustawić poniższe zmienne środowiskowe (np. w pliku `.env` lub eksportując je w terminalu):

```bash
# MSSQL
export MSSQL_SA_PWD=TwojeHasloMSSQL

# POSTGRESQL
export POSTGRES_USER=postgres
export POSTGRES_PWD=TwojeHasloPostgres
export POSTGRES_DB=bench

# CASSANDRA/SCYLLA
export CASSANDRA_USER=cassandra
export CASSANDRA_PWD=TwojeHasloCassandra
```

Możesz też utworzyć plik `.env` w katalogu głównym projektu z zawartością:

```
MSSQL_SA_PWD=TwojeHasloMSSQL
POSTGRES_USER=postgres
POSTGRES_PWD=TwojeHasloPostgres
POSTGRES_DB=bench
CASSANDRA_USER=cassandra
CASSANDRA_PWD=TwojeHasloCassandra
```

# Uruchom wszystkie usługi
cd /home/user/Projects/database-benchmarking
docker compose up --build -d

# Rebuild i restart tylko backendu (bez restartu baz)
docker compose up --build -d backend --no-deps

Uwaga:
- Backend nie wystawia `/actuator/health`.
- Skrypt benchmarkowy sprawdza gotowosc przez endpoint API:
   `http://localhost:8080/bookshop/1/books?db=POSTGRESQL&onlyAvailable=false`
- Schemat baz danych musi byc wgrany przed startem benchmarku; backend nie tworzy go automatycznie.

## Development
Komenda z uruchamiania. W przypadku chęci rebuildu backendu (przy już uruchomionych bazach danych) tak, aby zaoszczędzić czas - `docker compose up --build -d backend --no-deps`

## Przykladowe endpointy

## Generowanie danych

Aktualny generator danych jest w pliku `data_generator/generate_inserts.py`.

Podstawowe uruchomienie

```bash
python3 data_generator/generate_inserts.py -total-rows 500000 -engine postgresql
```

Szybkie ladowanie dla duzych zbiorow (PostgreSQL/MSSQL):

```bash
python3 data_generator/generate_inserts.py -total-rows 5000000 -engine postgresql -relational-load-mode bulk
python3 data_generator/generate_inserts.py -total-rows 5000000 -engine mssql -relational-load-mode bulk
```

Szybkie ladowanie dla NoSQL (Cassandra/Scylla):

```bash
python3 data_generator/generate_inserts.py -total-rows 5000000 -engine cassandra -nosql-load-mode bulk
python3 data_generator/generate_inserts.py -total-rows 5000000 -engine scylla -nosql-load-mode bulk
```

Powstaja paczki `generated_sql/bulk_<engine>_<rows>/` zawierajace:
- pliki CSV per tabela,
- skrypt ladowania: `load_cassandra_copy.cql` albo `load_scylla_copy.cql`.

Powstaja paczki `generated_sql/bulk_<engine>_<rows>/` zawierajace:
- pliki CSV per tabela,
- skrypt ladowania: `load_postgresql_copy.sql` albo `load_mssql_bulk_insert.sql`.

Po wykonaniu komendy plik wynikowy pojawi sie w katalogu `generated_sql/`.

### Dostepne parametry

- `-total-rows` - laczna liczba rekordow do wygenerowania (np. `500000`)
- `-size` - legacy parametr techniczny, nadal obslugiwany dla zgodnosci wstecznej
- `-engine` - wymagany silnik: `postgresql|mssql|cassandra|scylla`
- `-output-dir` - opcjonalny katalog wyjsciowy (domyslnie `generated_sql`)
- `-relational-load-mode` - dla `postgresql|mssql`: `inserts` (domyslnie) albo `bulk`
- `-nosql-load-mode` - dla `cassandra|scylla`: `inserts` (domyslnie) albo `bulk`

Uwaga: dla bardzo duzych wolumenow NoSQL (od 2 000 000) generator automatycznie przelacza tryb z `inserts` na `bulk`, aby uniknac zaciec procesu i bardzo duzego zuzycia RAM.

# Przykładowe uruchomienie endpointów benchmarkowych

Przed uruchomieniem testów wyczyść plik CSV (np. dla PostgreSQL):
```bash
curl -X POST "http://localhost:8080/benchmark/clear-csv?db=POSTGRESQL"
```

Przykładowe wywołania endpointów CRUD (dla PostgreSQL, z parametrami benchmarkowymi):

```bash
# R1: Pobierz książki z wybranego sklepu
curl "http://localhost:8080/bookshop/1/books?db=POSTGRESQL&operation=R1&iteration=2"
```

Kazdy endpoint jest wykonywany wielokrotnie. Domyslnie `run_benchmark.sh` uruchamia `1` iteracje warmup i `3` iteracje mierzone. Warmup nie wchodzi do pliku glownego CSV, a wszystkie pojedyncze przebiegi sa widoczne w pliku `_raw.csv`.

Mozesz sterowac liczba iteracji bez zmian w kodzie:

```bash
BENCH_WARMUP_ITERATIONS=2 BENCH_MEASURED_ITERATIONS=7 ./run_benchmark.sh MSSQL
```

---

## Ładowanie danych testowych do baz

### PostgreSQL

**Kolejność ładowania:**
1. Utwórz schemat:
   ```bash
   docker cp ./schema/sql/create_schema.sql postgres:/create_schema.sql
   docker exec -it postgres bash -c "psql -U postgres -d bench -f /create_schema.sql"
   ```
2. Utwórz tabele i całą strukturę:
   ```bash
   docker cp ./schema/sql/create_schema_structure.sql postgres:/create_schema_structure.sql
   docker exec -it postgres bash -c "psql -U postgres -d bench -f /create_schema_structure.sql"
   ```
3. Załaduj dane (szybki wariant COPY):
   ```bash
   python3 data_generator/generate_inserts.py -total-rows 5000000 -engine postgresql -relational-load-mode bulk
   docker compose cp ./generated_sql/bulk_postgresql_5000000 postgres:/tmp/bench_bulk
   docker compose exec -it postgres bash -c "psql -U $POSTGRES_USER -d $POSTGRES_DB -f /tmp/bench_bulk/load_postgresql_copy.sql"
   ```
4. Alternatywnie (legacy INSERT):
   ```bash
   docker cp ./generated_sql/inserts_postgresql_1000.sql postgres:/inserts_postgresql_1000.sql
   docker exec -it postgres bash -c "psql -U postgres -d bench -f /inserts_postgresql_1000.sql"
   ```

---

### MSSQL

**Kolejność ładowania:**
1. Utwórz schemat i tabele:
   ```bash
   docker cp ./schema/sql/create_schema.sql mssql:/create_schema.sql
   docker cp ./schema/sql/create_schema_structure.sql mssql:/create_schema_structure.sql
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /create_schema.sql"
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /create_schema_structure.sql"
   ```
2. Załaduj dane (szybki wariant BULK INSERT):
   ```bash
   DATASET_ROWS=5000000
   BULK_DIR="generated_sql/bulk_mssql_${DATASET_ROWS}"
   python3 data_generator/generate_inserts.py -total-rows "$DATASET_ROWS" -engine mssql -relational-load-mode bulk
   docker compose exec -T mssql mkdir -p /var/opt/mssql/import
   docker compose cp "$BULK_DIR"/. mssql:/var/opt/mssql/import
   docker compose exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -C -U sa -P '$MSSQL_SA_PWD' -d bench -i /var/opt/mssql/import/load_mssql_bulk_insert.sql"
   ```
3. Alternatywnie (legacy INSERT):
   ```bash
   docker cp ./generated_sql/inserts_mssql_1000.sql mssql:/inserts_mssql_1000.sql
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /inserts_mssql_1000.sql"
   ```

---

### Cassandra

**Kolejność ładowania:**
1. Utwórz schemat:
   ```bash
   docker run --rm --network cassandra -v "$PWD/schema/widecolumn:/schema:ro" cassandra:5.0.6 cqlsh cassandra -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /schema/create_schema.cql
   ```
2. Załaduj dane (szybki wariant COPY):
   ```bash
   python3 data_generator/generate_inserts.py -total-rows 5000000 -engine cassandra -nosql-load-mode bulk
   docker compose cp ./generated_sql/bulk_cassandra_5000000 cassandra:/tmp/bench_bulk
   docker compose exec -it cassandra /opt/cassandra/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /tmp/bench_bulk/load_cassandra_copy.cql
   ```
3. Alternatywnie (legacy INSERT):
   ```bash
   docker run --rm --network cassandra -v "$PWD/generated_sql:/data:ro" cassandra:5.0.6 cqlsh cassandra -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /data/inserts_cassandra_500000.cql
   ```

### Scylla

**Kolejność ładowania:**
1. Utwórz schemat:
   ```bash
   docker run --rm --network scylla --entrypoint /opt/scylladb/share/cassandra/bin/cqlsh -v "$PWD/schema/widecolumn:/schema:ro" scylladb/scylla:2026.1 scylla -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /schema/create_schema.cql
   ```
2. Załaduj dane (szybki wariant COPY):
   ```bash
   python3 data_generator/generate_inserts.py -total-rows 5000000 -engine scylla -nosql-load-mode bulk
   docker compose cp ./generated_sql/bulk_scylla_5000000 scylla:/tmp/bench_bulk
   docker compose exec -it scylla /usr/bin/cqlsh -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /tmp/bench_bulk/load_scylla_copy.cql
   ```
3. Alternatywnie (legacy INSERT):
   ```bash
   docker run --rm --network scylla --entrypoint /opt/scylladb/share/cassandra/bin/cqlsh -v "$PWD/generated_sql:/data:ro" scylladb/scylla:2026.1 scylla -u "$CASSANDRA_USER" -p "$CASSANDRA_PWD" -f /data/inserts_scylla_500000.cql
   ```

**Uwaga:**
- Najpierw zawsze ładuj pliki z definicją schematu i tabel, potem inserty z danymi.
- Jeśli masz inne pliki strukturalne (np. constraints, indexes), załaduj je przed insertami.
