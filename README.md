## Generowanie danych (generate_inserts)

Aktualny generator danych znajduje sie w pliku `data_generator/generate_inserts.py`.

Preferowane uruchomienie (nowy parametr):

```bash
python3 data_generator/generate_inserts.py -total-rows 500000 -engine postgresql
```

Plik wynikowy pojawi sie w katalogu `generated_sql/` (lub wskazanym przez `-output-dir`).

### Dostepne parametry

- `-engine` (wymagany): `postgresql|mssql|cassandra|scylla`
- `-total-rows` (zalecany): calkowita liczba rekordow do wygenerowania
- `-size` (legacy): preset rozmiaru (`1|2|3|4`), gdzie:
	`1=500000`, `2=1000000`, `3=5000000`, `4=10000000`
- `-output-dir` (opcjonalny): katalog wyjsciowy, domyslnie `generated_sql`


## Uruchamianie kodu Java (Spring Boot)

Modul Java znajduje sie w katalogu `bench/` (Maven + Spring Boot).

### Wymagania

- Java 21
- Maven 3.9+
- (opcjonalnie) Docker + Docker Compose do uruchomienia baz


### Szybki start PostgreSQL

Haslo do lokalnego PostgreSQL ustawione w projekcie to: `@testtest123A`.

1. Wystartuj PostgreSQL z Compose:

```bash
export POSTGRES_PWD='@testtest123A'
docker compose -f compose.yaml up -d postgres
```

2. Zainstaluj klienta `psql`, jesli go nie masz:

```bash
sudo apt update
sudo apt install postgresql-client
```

3. Wgraj schemat do bazy:

```bash
psql -h 127.0.0.1 -U postgres -d postgres -f schema/sql/create_schema.sql
psql -h 127.0.0.1 -U postgres -d postgres -f schema/sql/create_schema_structure.sql
```

4. W razie pytania podaj haslo:

```text
@testtest123A
```

5. Zbuduj aplikacje Java:

```bash
cd bench
mvn clean package -DskipTests
```

6. Uruchom aplikacje Java za pomoca JAR:

```bash
cd bench
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.engine=postgresql
```

Opcjonalnie: uruchomienie z automatycznym czyszczeniem wszystkich tabel schematu `bench`
i zaladowaniem wskazanego pliku z `generated_sql`:

```bash
cd bench
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.engine=postgresql --bench.load-sql=inserts_postgresql_250000.sql
```

Kolejnosc dzialania przy tych argumentach:

1. Rezolucja silnika bazodanowego (bench.engine),
2. TRUNCATE wszystkich tabel z `bench` (z `CASCADE`),
3. zaladowanie wskazanego pliku SQL,
4. start aplikacji.

Pliki logow CSV CRUD sa zapisywane w katalogu `bench/` jako:

- `postgresql_10000_create.csv`
- `postgresql_10000_read.csv`
- `postgresql_10000_update.csv`
- `postgresql_10000_delete.csv`

Nazwa pliku CSV uzaleznia sie od rozmiaru datasetu wziacieteqo z `bench.load-sql`.

### Dostepne endpointy API - User Account Permissions CRUD

Wszystkie operacje sa dedykowane tabelce `bench.useraccountpermissions` i sa dostepne dla PostgreSQL i MSSQL.
Logowanie do CSV dziala automatycznie.

#### POST /sql/user-account-permissions
Tworzy 3 rekordy (3 inserty z rownoleglem pomiarem czasowym). Srednia z 3 pomiarow jest zapisywana do `*_create.csv`.

```bash
curl -X POST http://localhost:8080/sql/user-account-permissions \
  -H "Content-Type: application/json" \
  -d '{"permission":"ADMIN","details":"test benchmark"}'
```

Odpowiedz:
```json
{
  "status": "created",
  "engine": "postgresql",
  "executions": 3,
  "ids": [1, 2, 3],
  "average_duration_ms": "2.6308",
  "permission": "ADMIN"
}
```

#### GET /sql/user-account-permissions/{id}
Odczytuje rekord o podanym ID. Pomiar czasu zapisywany do `*_read.csv`.

```bash
curl http://localhost:8080/sql/user-account-permissions/1
```

Odpowiedz:
```json
{
  "status": "read",
  "engine": "postgresql",
  "data": {
    "id": 1,
    "permission": "ADMIN",
    "details": "test benchmark"
  }
}
```

#### PUT /sql/user-account-permissions/{id}
Aktualizuje rekord o podanym ID. Pomiar czasu zapisywany do `*_update.csv`.

```bash
curl -X PUT http://localhost:8080/sql/user-account-permissions/1 \
  -H "Content-Type: application/json" \
  -d '{"permission":"USER","details":"updated"}'
```

Odpowiedz:
```json
{
  "status": "updated",
  "engine": "postgresql",
  "id": 1,
  "permission": "USER",
  "details": "updated"
}
```

#### DELETE /sql/user-account-permissions/{id}
Usuwa rekord o podanym ID. Pomiar czasu zapisywany do `*_delete.csv`.

```bash
curl -X DELETE http://localhost:8080/sql/user-account-permissions/1
```

Odpowiedz:
```json
{
  "status": "deleted",
  "engine": "postgresql",
  "id": 1
}
```

7. Sprawdz endpoint healthcheck:

```bash
curl http://localhost:8080/healthcheck
```

8. Zatrzymaj aplikacje dzialajaca na porcie 8080:

```bash
kill $(ss -lptn 'sport = :8080' | awk -F'pid=' 'NR>1{print $2}' | awk -F',' '{print $1}')
```

### Parametry aplikacji Java

Przy uruchamianiu aplikacji dostepne sa nastepujace parametry:

- `--bench.engine` (opcjonalny): `postgresql|mssql|cassandra|scylla`. Jesli nie podasz, aplikacja sprobuje wyodrebnic silnik z nazwy pliku `bench.load-sql`. Domyslnie PostgreSQL.
- `--bench.load-sql` (opcjonalny): nazwa lub sciezka do pliku SQL/CQL. Przy starcie dane zostan zaladowane do bazy (ze wcześniejszym TRUNCATE).
- Standardowe parametry Spring Boot: `--server.port=8080` itp.

Przyklady:

```bash
# Startuje na PostgreSQL (domyslnie)
java -jar target/app-0.0.1-SNAPSHOT.jar

# Startuje na PostgreSQL z zaladowaniem danych
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.load-sql=inserts_postgresql_250000.sql

# Startuje na MSSQL z zaladowaniem danych
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.engine=mssql --bench.load-sql=inserts_mssql_250000.sql

# Startuje na Cassandra (bez danych - CRUD nie jest jeszcze zaimplementowany dla NoSQL)
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.engine=cassandra
```

### Szybki start Cassandra

1. Uruchom Cassandra:

	docker compose -f compose.yaml up -d cassandra

2. Zaladuj schemat CQL:

	docker exec -i cassandra cqlsh < schema/widecolumn/create_schema.cql

Wazne: nie podawaj pliku jako argument po cqlsh, bo wtedy cqlsh traktuje to jako hostname i zwraca blad Name or service not known.

### Build i testy (Java)

```bash
cd bench
mvn clean package
mvn test
```

### Stan API Java

#### Architektura CRUD dla User Account Permissions

Implementacja nowego wzorca z ujednoliconym interfejsem CRUD, routerem silnika i katalogiem predefiniowanych zapytan:

- `UserPermissionCrudOperations` - unifikowany interfejs CRUD (create/read/update/delete)
- `UserPermissionCrudEngineService` - interfejs implementacji per silnik
- `BenchmarkEngineResolver` - rezolucja silnika na podstawie `bench.engine` lub `bench.load-sql`
- `UserPermissionQueryCatalog` - slownik predefiniowanych zapytan SQL per silnik i operacje
- `JdbcUserPermissionCrudService` - implementacja dla PostgreSQL i MSSQL (JDBC)
- `WideColumnUserPermissionCrudService` - punkt rozszerzenia dla Cassandra i Scylla (placeholder)
- `UserPermissionCrudServiceRouter` - router wybierajacy implementacje na podstawie resolwera silnika

**Zalety tego podejscia:**
- Wybor silnika odbywa sie raz przy starcie, bez ifow w kontrolerze
- Logika SQL jest oddzielona per silnik
- Logowanie CSV dziala na wszystkich operacjach (create z 3 powtorzeniami, read/update/delete pojedyncze pomiary)
- Nowe silniki mozna dopiacz przez implementacje `UserPermissionCrudEngineService` bez zmiany kontrolera

**Obsługiwane operacje:**
- CREATE: 3 inserty z pojedynczymi pomiarami + srednia -> CSV
- READ: pojedynczy pomiar -> CSV
- UPDATE: pojedynczy pomiar -> CSV
- DELETE: pojedynczy pomiar -> CSV

#### Inne endpointy

- Endpoint `GET /healthcheck` dziala i zwraca status aplikacji.
- Endpointy `BookShopController` sa w trakcie implementacji (serwisy aktualnie rzucaja `UnsupportedOperationException`).
- W `application.yaml` skonfigurowane sa jednoczesnie ustawienia PostgreSQL i Cassandra. Do pelnych testow endpointow SQL/NoSQL potrzebne sa odpowiednie bazy i kompletna implementacja serwisow.
