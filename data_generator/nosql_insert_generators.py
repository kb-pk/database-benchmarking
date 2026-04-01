"""Generatory danych dla silnikow NoSQL (Cassandra, ScyllaDB)."""

from __future__ import annotations


def generate_cassandra_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    """Szkielet pod przyszle inserty CQL dla Cassandra."""
    _ = table_row_overrides
    return (
        "-- Cassandra inserts skeleton\n"
        f"-- dataset_size={dataset_size}\n"
        "-- TODO: dodac generowanie CQL\n"
    )


def generate_scylla_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    """Szkielet pod przyszle inserty CQL dla ScyllaDB."""
    _ = table_row_overrides
    return (
        "-- ScyllaDB inserts skeleton\n"
        f"-- dataset_size={dataset_size}\n"
        "-- TODO: dodac generowanie CQL\n"
    )
