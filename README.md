Link do UML: https://lucid.app/lucidchart/87988d5e-6ca7-449b-be75-491117a453e3/edit?viewport_loc=351%2C48%2C2175%2C1087%2C0_0&invitationId=inv_b0529357-1922-4cad-b745-aa6f6c0f57ab 

## Uruchamianie
`docker compose up --build -d`

Wymagany procesor ze wsparciem dla SSE4.2 (flaga `sse4_2`) oraz PCLMUL (flaga `pclmulqdq`). 
Pierwsze uruchomienie będzie nieco wolniejsze (zapełnienie lokalnego repozytorium Maven).

## Development
Komenda z uruchamiania. W przypadku chęci rebuildu backendu (przy już uruchomionych bazach danych) tak, aby zaoszczędzić czas - `docker compose up --build -d backend`

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
