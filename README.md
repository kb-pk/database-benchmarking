Link do UML: https://lucid.app/lucidchart/87988d5e-6ca7-449b-be75-491117a453e3/edit?viewport_loc=351%2C48%2C2175%2C1087%2C0_0&invitationId=inv_b0529357-1922-4cad-b745-aa6f6c0f57ab

## Generowanie danych (generate_inserts)

Aktualny generator danych znajduje sie w pliku `data_generator/generate_inserts.py`.

Preferowane uruchomienie (nowy parametr):

```bash
python3 data_generator/generate_inserts.py -total-rows 500000 -engine postgresql
```

Tryb legacy (nadal wspierany):

```bash
python3 data_generator/generate_inserts.py -size 1 -engine postgresql
```

Plik wynikowy pojawi sie w katalogu `generated_sql/` (lub wskazanym przez `-output-dir`).

### Dostepne parametry

- `-engine` (wymagany): `postgresql|mssql|cassandra|scylla`
- `-total-rows` (zalecany): calkowita liczba rekordow do wygenerowania
- `-size` (legacy): preset rozmiaru (`1|2|3|4`), gdzie:
	`1=500000`, `2=1000000`, `3=5000000`, `4=10000000`
- `-output-dir` (opcjonalny): katalog wyjsciowy, domyslnie `generated_sql`

Uwagi:

- Musi byc podany przynajmniej jeden z parametrow: `-total-rows` albo `-size`.
- Jesli podasz oba, generator uzyje `-total-rows`.
- Dla `postgresql` i `mssql` generowany jest plik `.sql`.
- Dla `cassandra` i `scylla` generowany jest plik `.cql`.

Przyklady:

```bash
# PostgreSQL SQL
python3 data_generator/generate_inserts.py -total-rows 1000000 -engine postgresql

# MSSQL SQL
python3 data_generator/generate_inserts.py -size 1 -engine mssql

# Cassandra CQL
python3 data_generator/generate_inserts.py -total-rows 500000 -engine cassandra

# Wlasny katalog wyjsciowy
python3 data_generator/generate_inserts.py -total-rows 500000 -engine scylla -output-dir out
```

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

5. Uruchom aplikacje Java:

```bash
cd bench
mvn spring-boot:run
```

Opcjonalnie: uruchomienie z automatycznym czyszczeniem wszystkich tabel schematu `bench`
i zaladowaniem wskazanego pliku z `generated_sql`:

```bash
cd bench
mvn spring-boot:run -Dspring-boot.run.arguments="--bench.load-sql=inserts_postgresql_250000.sql"
```

Kolejnosc dzialania przy tych argumentach:

1. TRUNCATE wszystkich tabel z `bench` (z `CASCADE`),
2. zaladowanie wskazanego pliku SQL,
3. start aplikacji.

Pliki logow CSV CRUD sa zapisywane w katalogu `bench/` jako:

- `postgresql_1000_create.csv`
- `postgresql_1000_read.csv`
- `postgresql_1000_update.csv`
- `postgresql_1000_delete.csv`

Uwaga: sposob uruchamiania aplikacji nie zmienil sie.

6. Sprawdz endpoint healthcheck:

```bash
curl http://localhost:8080/healthcheck
```

7. Zatrzymaj aplikacje dzialajaca na porcie 8080:

```bash
kill $(ss -lptn 'sport = :8080' | awk -F'pid=' 'NR>1{print $2}' | awk -F',' '{print $1}')
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

- Endpoint `GET /healthcheck` dziala i zwraca status aplikacji.
- Endpointy `BookShopController` sa w trakcie implementacji (serwisy aktualnie rzucaja `UnsupportedOperationException`).
- W `application.yaml` skonfigurowane sa jednoczesnie ustawienia PostgreSQL i Cassandra. Do pelnych testow endpointow SQL/NoSQL potrzebne sa odpowiednie bazy i kompletna implementacja serwisow.
