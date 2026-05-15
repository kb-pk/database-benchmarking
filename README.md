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

Dla silnikow `cassandra` i `scylla` generator tworzy pelny plik `.cql` z insertami do tabel:
`bookshops`, `books_by_shop`, `users`, `user_credentials_by_login`, `employees_by_shop`,
`rentals_by_user`, `rentals_by_shop`, `reservations_by_user`.


## Uruchamianie kodu Java (Spring Boot)

Modul Java znajduje sie w katalogu `bench/` (Maven + Spring Boot).

### Wymagania

- Java 21
- Maven 3.9+
- (opcjonalnie) Docker + Docker Compose do uruchomienia baz

1. Zbuduj aplikacje Java:

```bash
cd bench
mvn clean package -DskipTests
```

2. Uruchom aplikacje Java za pomoca JAR:

Uruchomienie z automatycznym czyszczeniem wszystkich tabel schematu `bench` i zaladowaniem wskazanego pliku z `generated_sql`:

```bash
cd bench
java -jar target/app-0.0.1-SNAPSHOT.jar --bench.engine=postgresql --bench.load-sql=inserts_postgresql_250000.sql
```

Kolejnosc dzialania przy tych argumentach:

Pliki logow CSV CRUD sa zapisywane w katalogu `../output_data/{engine}/`
Nazwa pliku CSV uzaleznia sie od rozmiaru datasetu wziateqo z `bench.load-sql`.
Po zaladowaniu danych aplikacja automatycznie uruchamia benchmark CREATE (wszystkie 9 operacji) i zapisuje wyniki do plikow CSV.


3. Sprawdz endpoint healthcheck:

```bash
curl http://localhost:8080/healthcheck
```

4. Zatrzymaj aplikacje dzialajaca na porcie 8080:

```bash
kill $(ss -lptn 'sport = :8080' | awk -F'pid=' 'NR>1{print $2}' | awk -F',' '{print $1}')
```

### Parametry aplikacji Java

Przy uruchamianiu aplikacji dostepne sa nastepujace parametry:

- `--bench.engine` (opcjonalny): `postgresql|mssql|cassandra|scylla`. Jesli nie podasz, aplikacja sprobuje wyodrebnic silnik z nazwy pliku `bench.load-sql`. Domyslnie PostgreSQL.
- `--bench.load-sql` (opcjonalny): nazwa lub sciezka do pliku SQL/CQL. Przy starcie dane zostan zaladowane do bazy (ze wcześniejszym TRUNCATE).
- Standardowe parametry Spring Boot: `--server.port=8080` itp.

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
- CREATE 1-9: Operacje tworzenia w bazie (każda z 3 powtórzeniami + średnia) -> CSV
  - CREATE 1-6: Podstawowe operacje insert na różne tabele
  - CREATE 7: Rental z pełnym kontekstem - rejestruje wypożyczenie z rezolucją 5 kluczy obcych (book, user, employee, shop, rental method)
  - CREATE 8: Rental warunkowy - szuka aktywnego użytkownika z książką w jego sklepie, fallback na każdego użytkownika
  - CREATE 9: Batch supply event - wstawia wiele książek w jednej operacji (domyślnie 5 książek × 3 powtórzenia = 15 książek)
- READ: Pojedynczy pomiar -> CSV
- UPDATE: Pojedynczy pomiar -> CSV
- DELETE: Pojedynczy pomiar -> CSV

#### Inne endpointy

- Endpoint `GET /healthcheck` dziala i zwraca status aplikacji.
- Endpointy `BookShopController` sa w trakcie implementacji (serwisy aktualnie rzucaja `UnsupportedOperationException`).
- W `application.yaml` skonfigurowane sa jednoczesnie ustawienia PostgreSQL i Cassandra. Do pelnych testow endpointow SQL/NoSQL potrzebne sa odpowiednie bazy i kompletna implementacja serwisow.
