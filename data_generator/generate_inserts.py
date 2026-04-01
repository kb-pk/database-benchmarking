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
        required=True,
        choices=SIZE_CHOICES,
        help="Rozmiar zbioru danych.",
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

    print("=== Generator insertow (szkielet) ===")

    print("\nWybrane parametry:")
    print(f"- Rozmiar zbioru: {args.size}")
    print(f"- Silnik: {args.engine}")

    engine_family = ENGINE_FAMILIES[args.engine]
    args.output_dir.mkdir(parents=True, exist_ok=True)

    if engine_family == "relational":
        generated_content = generate_relational_sql(args.size, None)
        file_extension = "sql"
    elif engine_family == "cassandra":
        generated_content = generate_cassandra_cql(args.size, None)
        file_extension = "cql"
    elif engine_family == "scylla":
        generated_content = generate_scylla_cql(args.size, None)
        file_extension = "cql"
    else:
        raise ValueError(f"Unsupported engine family: {engine_family}")

    output_path = args.output_dir / f"inserts_{args.engine}_{args.size}.{file_extension}"
    output_path.write_text(generated_content, encoding="utf-8")

    print(f"- Plik wynikowy: {output_path}")
    print("\nSzkielet generatora zostal uruchomiony poprawnie.")


if __name__ == "__main__":
    main()