"""Generatory plikow SQL z insertami.

Ten modul na tym etapie generuje:
- tabele bez kluczy obcych,
- kolejne tabele zalezne zgodnie z bezpieczna kolejnoscia ladowania.

Wymaganie: PostgreSQL i MSSQL dostaja identyczna tresc SQL.
"""

from __future__ import annotations


RELATIONAL_TABLES = (
    "ActivationStatus",
    "UserAccountPermissions",
    "BookRentalMethod",
    "BookShopUser",
)

ACTIVATION_STATUSES = ["ACTIVE", "INACTIVE", "PENDING", "SUSPENDED", "ARCHIVED"]
BOOK_RENTAL_METHODS = ["Książkomat", "Wypożyczalnia"]

# Tymczasowy globalny rozmiar danych dla tabel "duzych".
DEFAULT_ROW_COUNT = 500

# Prosta, bezposrednia konfiguracja liczby rekordow per tabela.
TABLE_ROW_COUNTS = {
    "ActivationStatus": len(ACTIVATION_STATUSES),
    "UserAccountPermissions": 5,
    "BookRentalMethod": len(BOOK_RENTAL_METHODS),
    "BookShopUser": DEFAULT_ROW_COUNT,
}


def _sql_quote(value: str) -> str:
    """Escapuje apostrofy do bezpiecznego literału SQL."""
    return value.replace("'", "''")


def resolve_table_row_counts(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> dict[str, int]:
    """Wylicza docelowa liczbe rekordow dla kazdej wspieranej tabeli."""
    _ = dataset_size
    counts = dict(TABLE_ROW_COUNTS)
    if table_row_overrides:
        for table_name, row_count in table_row_overrides.items():
            if table_name not in counts:
                raise ValueError(f"Unsupported table override: {table_name}")
            if row_count <= 0:
                raise ValueError(
                    f"Row count for {table_name} must be greater than zero.",
                )
            counts[table_name] = row_count
    return counts


def generate_activation_status_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.ActivationStatus.

    Tabela ma zamkniety slownik statusow bez powtorzen.
    """
    max_statuses = len(ACTIVATION_STATUSES)
    if row_count > max_statuses:
        raise ValueError(
            "bench.ActivationStatus obsluguje maksymalnie 5 unikalnych rekordow.",
        )

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        status = ACTIVATION_STATUSES[row_id - 1]
        lines.append(
            "INSERT INTO bench.ActivationStatus (id, status) "
            f"VALUES ({row_id}, '{_sql_quote(status)}');"
        )
    return lines


def generate_user_account_permissions_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.UserAccountPermissions.

    Tabela ma zamkniety slownik 5 unikalnych rekordow bez powtorzen.
    """
    permission_types = [
        ("WYPOZYCZENIE", "Mozliwosc wypozyczania ksiazek"),
        ("REZERWACJA_I_WYPOZYCZENIE", "Mozliwosc rezerwacji i wypozyczania ksiazek"),
        ("ZABLOKOWANE_KONTO", "Konto zablokowane - brak operacji"),
        ("TYLKO_REZERWACJA", "Mozliwosc tylko rezerwowania ksiazek"),
        ("PELNY_DOSTEP", "Pelny dostep do wszystkich operacji"),
    ]

    max_permissions = len(permission_types)
    if row_count > max_permissions:
        raise ValueError(
            "bench.UserAccountPermissions obsluguje maksymalnie 5 unikalnych rekordow.",
        )

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        permission, details = permission_types[row_id - 1]
        lines.append(
            "INSERT INTO bench.UserAccountPermissions (id, permission, details) "
            f"VALUES ({row_id}, '{_sql_quote(permission)}', '{_sql_quote(details)}');"
        )
    return lines


def generate_book_rental_method_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.BookRentalMethod.

    Tabela ma zamkniety slownik 2 unikalnych rekordow bez powtorzen.
    """
    max_methods = len(BOOK_RENTAL_METHODS)
    if row_count > max_methods:
        raise ValueError(
            "bench.BookRentalMethod obsluguje maksymalnie 2 unikalne rekordy.",
        )

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        method = BOOK_RENTAL_METHODS[row_id - 1]
        lines.append(
            "INSERT INTO bench.BookRentalMethod (id, method) "
            f"VALUES ({row_id}, '{_sql_quote(method)}');"
        )
    return lines


def generate_book_shop_user_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.BookShopUser.

    Szkielet: mainBookShopId ustawiamy na NULL, aby nie wymagac jeszcze danych BookShop.
    """
    first_names = [
        "Piotr", "Anna", "Krzysztof", "Maria", "Andrzej",
        "Katarzyna", "Tomasz", "Malgorzata", "Pawel", "Agnieszka",
        "Jan", "Barbara", "Michal", "Ewa", "Marcin",
        "Magdalena", "Jakub", "Elzbieta", "Adam", "Joanna",
    ]
    surnames = [
        "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk",
        "Kaminski", "Lewandowski", "Zielinski", "Szymanski", "Wozniak",
        "Dabrowski", "Kozlowski", "Mazur", "Jankowski", "Kwiatkowski",
        "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski", "Zajac",
        "Pawlowski", "Michalski", "Krol", "Wieczorek", "Jablonski",
        "Wrobel", "Nowakowski", "Majewski", "Olszewski", "Stepien",
        "Malinowski", "Jaworski", "Adamczyk", "Dudek", "Nowicki",
        "Pawlak", "Witkowski", "Walczak", "Sikora", "Baran",
    ]

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        name = first_names[(row_id - 1) % len(first_names)]
        surname = surnames[(row_id * 7 - 1) % len(surnames)]
        phone_number = f"+48{row_id % 1_000_000_000:09d}"
        email = f"{name.lower()}.{surname.lower()}{row_id}@poczta.pl"
        is_active_id = ((row_id - 1) % 5) + 1
        lines.append(
            "INSERT INTO bench.BookShopUser "
            "(id, name, surname, phoneNumber, email, mainBookShopId, isActiveId) "
            f"VALUES ({row_id}, '{_sql_quote(name)}', '{_sql_quote(surname)}', "
            f"'{_sql_quote(phone_number)}', '{_sql_quote(email)}', NULL, {is_active_id});"
        )
    return lines


def build_shared_insert_lines(table_row_counts: dict[str, int]) -> list[str]:
    """Buduje wspolna tresc SQL dla wszystkich wspieranych silnikow."""
    lines: list[str] = [
        "-- Generic inserts for relational engines",
        "-- Stage 1: tables without foreign keys",
        "",
    ]

    # 1) ActivationStatus
    activation_status_count = table_row_counts["ActivationStatus"]
    lines.append(f"-- ActivationStatus: {activation_status_count} rows")
    lines.extend(generate_activation_status_inserts(activation_status_count))
    lines.append("")

    # 2) UserAccountPermissions
    user_account_permissions_count = table_row_counts["UserAccountPermissions"]
    lines.append(f"-- UserAccountPermissions: {user_account_permissions_count} rows")
    lines.extend(generate_user_account_permissions_inserts(user_account_permissions_count))
    lines.append("")

    # 3) BookRentalMethod
    book_rental_method_count = table_row_counts["BookRentalMethod"]
    lines.append(f"-- BookRentalMethod: {book_rental_method_count} rows")
    lines.extend(generate_book_rental_method_inserts(book_rental_method_count))
    lines.append("")

    # 4) BookShopUser
    lines.append("-- Stage 2: tables with minimal dependencies")
    book_shop_user_count = table_row_counts["BookShopUser"]
    lines.append(f"-- BookShopUser: {book_shop_user_count} rows")
    lines.extend(generate_book_shop_user_inserts(book_shop_user_count))
    lines.append("")

    return lines


def generate_relational_sql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    """Zwraca tresc insertow dla silnikow relacyjnych (PostgreSQL/MSSQL)."""
    table_row_counts = resolve_table_row_counts(dataset_size, table_row_overrides)
    return "\n".join(build_shared_insert_lines(table_row_counts)) + "\n"