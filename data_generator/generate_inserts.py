#!/usr/bin/env python3

"""Szkielet generatora insertow dla benchmarkow bazy danych.

Ten plik na razie tylko zbiera parametry uruchomienia z flag CLI:
- rozmiar zbioru danych,
- typ silnika.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from nosql_insert_generators import (
    generate_cassandra_copy_script,
    generate_scylla_copy_script,
    write_cassandra_cql_file,
    write_nosql_bulk_csv_files,
    write_scylla_cql_file,
)
from sql_insert_generators import (
    build_relational_post_load_updates,
    calculate_table_row_counts,
    generate_mssql_bulk_insert_script,
    generate_postgresql_copy_script,
    write_relational_bulk_csv_files,
    generate_relational_sql,
)


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
    parser.add_argument(
        "-relational-load-mode",
        type=str,
        default="inserts",
        choices=("inserts", "bulk"),
        help="Tryb dla PostgreSQL/MSSQL: inserts (domyslnie) albo bulk (CSV + COPY/BULK INSERT).",
    )
    parser.add_argument(
        "-mssql-batch-size",
        type=int,
        default=2000,
        help="Rozmiar BATCHSIZE dla MSSQL BULK INSERT (domyslnie 2000).",
    )
    parser.add_argument(
        "-mssql-rows-per-batch",
        type=int,
        default=2000,
        help="Rozmiar ROWS_PER_BATCH dla MSSQL BULK INSERT (domyslnie 2000).",
    )
    parser.add_argument(
        "-nosql-load-mode",
        type=str,
        default="inserts",
        choices=("inserts", "bulk"),
        help="Tryb dla Cassandra/Scylla: inserts (domyslnie) albo bulk (CSV + COPY).",
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
        if args.relational_load_mode == "bulk":
            bundle_dir = args.output_dir / f"bulk_{args.engine}_{dataset_size_for_output}"
            if use_total_rows:
                table_row_counts = calculate_table_row_counts(args.total_rows)
            else:
                raise ValueError("Tryb bulk wymaga -total-rows")

            write_relational_bulk_csv_files(bundle_dir, table_row_counts)
            post_load_updates = build_relational_post_load_updates(table_row_counts)

            if args.engine == "postgresql":
                loader_path = bundle_dir / "load_postgresql_copy.sql"
                loader_path.write_text(
                    generate_postgresql_copy_script(table_row_counts, post_load_updates),
                    encoding="utf-8",
                )
            elif args.engine == "mssql":
                loader_path = bundle_dir / "load_mssql_bulk_insert.sql"
                loader_path.write_text(
                    generate_mssql_bulk_insert_script(
                        table_row_counts,
                        post_load_updates,
                        batch_size=args.mssql_batch_size,
                        rows_per_batch=args.mssql_rows_per_batch,
                    ),
                    encoding="utf-8",
                )
            else:
                raise ValueError(f"Bulk mode nie jest wspierany dla silnika: {args.engine}")

            print(f"- Pakiet BULK zapisany w: {bundle_dir}")
            print(f"- Skrypt ladowania: {loader_path}")
            print("\nSzkielet generatora zostal uruchomiony poprawnie.")
            return

        if use_total_rows:
            generated_content = generate_relational_sql(args.size or 0, None, args.total_rows)
        else:
            generated_content = generate_relational_sql(args.size, None)
        file_extension = "sql"
    elif engine_family == "cassandra":
        if args.nosql_load_mode == "inserts" and dataset_size_for_output >= 2_000_000:
            print("WARNING: Dla Cassandra i bardzo duzych zbiorow inserts moze zacinac proces.")
            print("WARNING: Automatycznie przelaczam -nosql-load-mode na bulk.")
            args.nosql_load_mode = "bulk"

        if args.nosql_load_mode == "bulk":
            bundle_dir = args.output_dir / f"bulk_{args.engine}_{dataset_size_for_output}"
            write_nosql_bulk_csv_files(bundle_dir, dataset_size_for_output)
            loader_path = bundle_dir / "load_cassandra_copy.cql"
            loader_path.write_text(generate_cassandra_copy_script(dataset_size_for_output), encoding="utf-8")
            print(f"- Pakiet BULK zapisany w: {bundle_dir}")
            print(f"- Skrypt ladowania: {loader_path}")
            print("\nSzkielet generatora zostal uruchomiony poprawnie.")
            return
        output_path = args.output_dir / f"inserts_{args.engine}_{dataset_size_for_output}.cql"
        write_cassandra_cql_file(output_path, dataset_size_for_output, None)
        file_extension = "cql"
    elif engine_family == "scylla":
        if args.nosql_load_mode == "inserts" and dataset_size_for_output >= 2_000_000:
            print("WARNING: Dla Scylla i bardzo duzych zbiorow inserts moze zacinac proces.")
            print("WARNING: Automatycznie przelaczam -nosql-load-mode na bulk.")
            args.nosql_load_mode = "bulk"

        if args.nosql_load_mode == "bulk":
            bundle_dir = args.output_dir / f"bulk_{args.engine}_{dataset_size_for_output}"
            write_nosql_bulk_csv_files(bundle_dir, dataset_size_for_output)
            loader_path = bundle_dir / "load_scylla_copy.cql"
            loader_path.write_text(generate_scylla_copy_script(dataset_size_for_output), encoding="utf-8")
            print(f"- Pakiet BULK zapisany w: {bundle_dir}")
            print(f"- Skrypt ladowania: {loader_path}")
            print("\nSzkielet generatora zostal uruchomiony poprawnie.")
            return
        output_path = args.output_dir / f"inserts_{args.engine}_{dataset_size_for_output}.cql"
        write_scylla_cql_file(output_path, dataset_size_for_output, None)
        file_extension = "cql"
    else:
        raise ValueError(f"Unsupported engine family: {engine_family}")

    if engine_family == "relational":
        output_path = args.output_dir / f"inserts_{args.engine}_{dataset_size_for_output}.{file_extension}"
        output_path.write_text(generated_content, encoding="utf-8")
    else:
        output_path = args.output_dir / f"inserts_{args.engine}_{dataset_size_for_output}.{file_extension}"

    print(f"- Plik wynikowy: {output_path}")
    print("\nSzkielet generatora zostal uruchomiony poprawnie.")


if __name__ == "__main__":
    main()