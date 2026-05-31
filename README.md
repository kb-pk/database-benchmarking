Link do UML: https://lucid.app/lucidchart/87988d5e-6ca7-449b-be75-491117a453e3/edit?viewport_loc=351%2C48%2C2175%2C1087%2C0_0&invitationId=inv_b0529357-1922-4cad-b745-aa6f6c0f57ab 

## Projekt w skrocie
- Benchmark jest wykonywany przez endpointy HTTP backendu
- Wyniki czasu operacji CRUD sa zapisywane do CSV w `output_data/<silnik>/crud_timings_<silnik>.csv`.
- Kazdy test ma warmup (cold start): iteracja `1` jest pomijana w CSV, raportowane sa iteracje `2..4`.
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

# Restart tylko backendu po przebudowie
```
docker compose down -v
cd bench
mvn clean package -DskipTests
cd ..
docker compose restart backend
```

## Development
Komenda z uruchamiania. W przypadku chęci rebuildu backendu (przy już uruchomionych bazach danych) tak, aby zaoszczędzić czas - `docker compose up --build -d backend --no-deps`

Uruchomienie przykladowych endpointow

## Generowanie danych

Aktualny generator danych jest w pliku `data_generator/generate_inserts.py`.

Podstawowe uruchomienie

```bash
python3 data_generator/generate_inserts.py -size 500000 -engine postgresql
```

Po wykonaniu komendy plik wynikowy pojawi sie w katalogu `generated_sql/`.

### Dostepne parametry

- `-size` - wymagany parametr techniczny uruchomienia (np. `500000`)
- `-engine` - wymagany silnik: `postgresql|mssql|cassandra|scylla`
- `-output-dir` - opcjonalny katalog wyjsciowy (domyslnie `generated_sql`)

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

Każdy endpoint należy wywołać 4 razy z parametrem `iteration=1..4` (pierwsze wywołanie to warmup, nie jest logowane do CSV).

---

## Ładowanie danych testowych do baz

### PostgreSQL

# Ładowanie struktury bazy i danych testowych do PostgreSQL

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
3. Załaduj dane:
   ```bash
   docker cp ./generated_sql/inserts_postgresql_1000.sql postgres:/inserts_postgresql_1000.sql
   docker exec -it postgres bash -c "psql -U postgres -d bench -f /inserts_postgresql_1000.sql"
   ```

---

### MSSQL

# Ładowanie struktury bazy i danych testowych do MSSQL

**Kolejność ładowania:**
1. Utwórz schemat i tabele:
   ```bash
   docker cp ./schema/sql/create_schema.sql mssql:/create_schema.sql
   docker cp ./schema/sql/create_schema_structure.sql mssql:/create_schema_structure.sql
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /create_schema.sql"
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /create_schema_structure.sql"
   ```
2. Załaduj dane:
   ```bash
   docker cp ./generated_sql/inserts_mssql_1000.sql mssql:/inserts_mssql_1000.sql
   docker exec -it mssql bash -c "/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P '@testtest123A' -d bench -i /inserts_mssql_1000.sql"
   ```

---

**Uwaga:**
- Najpierw zawsze ładuj pliki z definicją schematu i tabel, potem inserty z danymi.
- Jeśli masz inne pliki strukturalne (np. constraints, indexes), załaduj je przed insertami.
