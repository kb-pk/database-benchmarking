#!/usr/bin/env python3

"""Szkielet generatora insertow dla benchmarkow bazy danych.

Ten plik na razie tylko zbiera parametry uruchomienia z flag CLI:
- rozmiar zbioru danych,
- typ silnika.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from nosql_insert_generators import generate_cassandra_cql, generate_scylla_cql
from sql_insert_generators import generate_relational_sql


DATASET_OPTIONS = {
    "1": 500_000,
    "2": 1_000_000,
    "3": 5_000_000,
    "4": 10_000_000,
}

ENGINE_OPTIONS = {
    "1": "postgresql",
    "2": "mssql",
    "3": "cassandra",
    "4": "scylla",
}

ENGINE_FAMILIES = {
    "postgresql": "relational",
    "mssql": "relational",
    "cassandra": "cassandra",
    "scylla": "scylla",
}


SIZE_CHOICES = sorted(DATASET_OPTIONS.values())
ENGINE_CHOICES = tuple(ENGINE_OPTIONS.values())


def parse_args() -> argparse.Namespace:
    """Parsuje argumenty przekazane w linii polecen."""
    parser = argparse.ArgumentParser(
        description="Generator insertow (szkielet) - wybor rozmiaru i silnika.",
    )
    parser.add_argument(
        "-size",
        type=int,
        required=False,
        choices=SIZE_CHOICES,
        help="Rozmiar zbioru danych (PRZESTARZAŁE - użyj -total-rows).",
    )
    parser.add_argument(
        "-total-rows",
        type=int,
        required=False,
        help="Całkowita liczba rekordów do wygenerowania. System automatycznie wylicza liczbę dla każdej tabeli.",
    )
    parser.add_argument(
        "-engine",
        type=str,
        required=True,
        choices=ENGINE_CHOICES,
        help="Typ silnika bazy danych.",
    )
    parser.add_argument(
        "-output-dir",
        type=Path,
        default=Path("generated_sql"),
        help="Katalog wyjsciowy na wygenerowane pliki SQL.",
    )
    return parser.parse_args()


def main() -> None:
    """Punkt startowy aplikacji."""
    args = parse_args()
    
    # Sprawdź parametry
    if args.total_rows is None and args.size is None:
        print("ERROR: Musisz podać -total-rows lub -size")
        return
    
    if args.total_rows is not None and args.size is not None:
        print("WARNING: Obie flagi (-total-rows i -size) podane. Usunę -size i użyję -total-rows")
        args.size = None

    print("=== Generator insertow (szkielet) ===")

    if args.total_rows is not None:
        print("\nWybrane parametry:")
        print(f"- Całkowita liczba rekordów: {args.total_rows}")
        dataset_size_for_output = args.total_rows
        use_total_rows = True
    else:
        print("\nWybrane parametry:")
        print(f"- Rozmiar zbioru: {args.size}")
        dataset_size_for_output = args.size
        use_total_rows = False
    
    print(f"- Silnik: {args.engine}")

    engine_family = ENGINE_FAMILIES[args.engine]
    args.output_dir.mkdir(parents=True, exist_ok=True)

    if engine_family == "relational":
        if use_total_rows:
            generated_content = generate_relational_sql(args.size or 0, None, args.total_rows)
        else:
            generated_content = generate_relational_sql(args.size, None)
        file_extension = "sql"
    elif engine_family == "cassandra":
        generated_content = generate_cassandra_cql(dataset_size_for_output, None)
        file_extension = "cql"
    elif engine_family == "scylla":
        generated_content = generate_scylla_cql(dataset_size_for_output, None)
        file_extension = "cql"
    else:
        raise ValueError(f"Unsupported engine family: {engine_family}")

    output_path = args.output_dir / f"inserts_{args.engine}_{dataset_size_for_output}.{file_extension}"
    output_path.write_text(generated_content, encoding="utf-8")
    print(f"- Plik wynikowy: {output_path}")
    
    print("\nSzkielet generatora zostal uruchomiony poprawnie.")

    print(f"- Plik wynikowy: {output_path}")
    print("\nSzkielet generatora zostal uruchomiony poprawnie.")


if __name__ == "__main__":
    main()