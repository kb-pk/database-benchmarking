"""Generatory danych dla silnikow NoSQL (Cassandra, ScyllaDB)."""

from __future__ import annotations

from datetime import date, timedelta
import uuid


BOOKSHOP_COUNT = 10

BOOKSHOP_NAMES = [
    "Book Haven",
    "Readers Paradise",
    "The Book Nook",
    "Page Turners",
    "Literary Lounge",
    "Weekend Reads",
    "Silent Shelf",
    "Open Chapter",
    "Amber Pages",
    "Midnight Stories",
]

BOOKSHOP_ADDRESSES = [
    "ul. Dluga 12, Warszawa",
    "ul. Lipowa 8, Krakow",
    "ul. Ogrodowa 19, Gdansk",
    "ul. Sosnowa 4, Wroclaw",
    "ul. Klonowa 21, Poznan",
    "ul. Morska 15, Gdynia",
    "ul. Kwiatowa 3, Lublin",
    "ul. Rynek 7, Szczecin",
    "ul. Sloneczna 18, Torun",
    "ul. Wiosenna 25, Katowice",
]

FIRST_NAMES = [
    "Piotr", "Anna", "Krzysztof", "Maria", "Andrzej", "Katarzyna", "Tomasz", "Malgorzata", "Pawel", "Agnieszka",
    "Jan", "Barbara", "Michal", "Ewa", "Marcin", "Magdalena", "Jakub", "Elzbieta", "Adam", "Joanna",
]

SURNAMES = [
    "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk", "Kaminski", "Lewandowski", "Zielinski", "Szymanski", "Wozniak",
    "Dabrowski", "Kozlowski", "Mazur", "Jankowski", "Kwiatkowski", "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski", "Zajac",
]

PUBLISHERS = [
    "PWN",
    "Helion",
    "Znak",
    "Rebis",
    "Albatros",
    "Czarne",
    "Marginesy",
]

STATUSES = ["ACTIVE", "INACTIVE", "PENDING", "SUSPENDED"]
PERMISSION_SETS = [
    "{'WYPOZYCZENIE'}",
    "{'REZERWACJA_I_WYPOZYCZENIE'}",
    "{'TYLKO_REZERWACJA'}",
    "{'PELNY_DOSTEP'}",
]
RENTAL_METHODS = ["Ksiazkkomat", "Wypozyczalnia"]


def _q(value: str) -> str:
    return value.replace("'", "''")


def _det_uuid(prefix: str, index: int) -> uuid.UUID:
    return uuid.uuid5(uuid.NAMESPACE_DNS, f"{prefix}-{index}")


def _clamp(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def _resolve_counts(dataset_size: int, table_row_overrides: dict[str, int] | None = None) -> dict[str, int]:
    base = {
        "bookshops": BOOKSHOP_COUNT,
        "users": _clamp(dataset_size // 12, 100, 250_000),
        "books_by_shop": _clamp(dataset_size // 8, 200, 400_000),
        "employees_by_shop": _clamp(dataset_size // 20, 80, 80_000),
        "rentals": _clamp(dataset_size // 5, 300, 600_000),
        "reservations_by_user": _clamp(dataset_size // 7, 200, 350_000),
    }
    base["user_credentials_by_login"] = base["users"]

    if table_row_overrides:
        for table_name, row_count in table_row_overrides.items():
            if table_name not in base:
                raise ValueError(f"Unsupported table override: {table_name}")
            if row_count <= 0:
                raise ValueError(f"Row count for {table_name} must be greater than zero")
            base[table_name] = row_count

    base["bookshops"] = BOOKSHOP_COUNT
    base["user_credentials_by_login"] = base["users"]
    return base


def _generate_bookshops(lines: list[str], shop_ids: list[uuid.UUID]) -> None:
    for i, shop_id in enumerate(shop_ids):
        name = BOOKSHOP_NAMES[i]
        address = BOOKSHOP_ADDRESSES[i]
        email = f"shop{i + 1}@bench.local"
        manager_id = _det_uuid("manager", i + 1)
        lines.append(
            "INSERT INTO bookshops ("
            "shop_id, shop_name, address, email, manager_id, "
            "opens_at_monday, closes_at_monday, "
            "opens_at_tuesday, closes_at_tuesday, "
            "opens_at_wednesday, closes_at_wednesday, "
            "opens_at_thursday, closes_at_thursday, "
            "opens_at_friday, closes_at_friday, "
            "opens_at_saturday, closes_at_saturday, "
            "opens_at_sunday, closes_at_sunday"
            ") VALUES ("
            f"{shop_id}, '{_q(name)}', '{_q(address)}', '{email}', {manager_id}, "
            "'08:00:00', '20:00:00', "
            "'08:00:00', '20:00:00', "
            "'08:00:00', '20:00:00', "
            "'08:00:00', '20:00:00', "
            "'08:00:00', '20:00:00', "
            "'09:00:00', '18:00:00', "
            "'10:00:00', '16:00:00'"
            ");"
        )


def _generate_users(lines: list[str], user_count: int, shop_ids: list[uuid.UUID]) -> list[uuid.UUID]:
    user_ids: list[uuid.UUID] = []
    for i in range(1, user_count + 1):
        user_id = _det_uuid("user", i)
        user_ids.append(user_id)
        first = FIRST_NAMES[(i - 1) % len(FIRST_NAMES)]
        surname = SURNAMES[(i - 1) % len(SURNAMES)]
        login = f"user{i:07d}"
        status = STATUSES[(i - 1) % len(STATUSES)]
        permissions = PERMISSION_SETS[(i - 1) % len(PERMISSION_SETS)]
        phone = f"+48500{i % 1_000_000:06d}"
        email = f"{first.lower()}.{surname.lower()}{i}@bench.local"
        card = f"CARD-{i:09d}"
        main_shop_id = shop_ids[(i - 1) % len(shop_ids)]

        lines.append(
            "INSERT INTO users ("
            "user_id, name, surname, phone_number, email, main_book_shop_id, "
            "card_id_number, status, login, permissions"
            ") VALUES ("
            f"{user_id}, '{_q(first)}', '{_q(surname)}', '{phone}', '{email}', {main_shop_id}, "
            f"'{card}', '{status}', '{login}', {permissions}"
            ");"
        )

        pwd_hash = f"hash_{uuid.uuid5(uuid.NAMESPACE_DNS, login)}"
        lines.append(
            "INSERT INTO user_credentials_by_login (login, user_id, password_hash, status) VALUES ("
            f"'{login}', {user_id}, '{pwd_hash}', '{status}'"
            ");"
        )

    return user_ids


def _generate_employees(lines: list[str], employee_count: int, shop_ids: list[uuid.UUID], base_date: date) -> list[uuid.UUID]:
    employee_ids: list[uuid.UUID] = []
    for i in range(1, employee_count + 1):
        employee_id = _det_uuid("employee", i)
        employee_ids.append(employee_id)
        shop_id = shop_ids[(i - 1) % len(shop_ids)]
        first = FIRST_NAMES[(i + 3) % len(FIRST_NAMES)]
        surname = SURNAMES[(i + 7) % len(SURNAMES)]
        phone = f"+48600{i % 1_000_000:06d}"
        email = f"emp.{first.lower()}.{surname.lower()}{i}@bench.local"
        birth_date = base_date - timedelta(days=365 * (22 + (i % 30)))
        started_at = base_date - timedelta(days=(i % 3650))
        role = "Kierownik" if i % 10 == 0 else "Ksiegarnia"

        lines.append(
            "INSERT INTO employees_by_shop ("
            "primary_book_shop_id, employee_id, name, surname, phone_number, email, "
            "birth_date, started_at, primary_business_role"
            ") VALUES ("
            f"{shop_id}, {employee_id}, '{_q(first)}', '{_q(surname)}', '{phone}', '{email}', "
            f"'{birth_date.isoformat()}', '{started_at.isoformat()}', '{role}'"
            ");"
        )

    return employee_ids


def _generate_books(lines: list[str], book_count: int, shop_ids: list[uuid.UUID], base_date: date) -> list[tuple[uuid.UUID, uuid.UUID, str]]:
    books: list[tuple[uuid.UUID, uuid.UUID, str]] = []
    for i in range(1, book_count + 1):
        shop_id = shop_ids[(i - 1) % len(shop_ids)]
        book_id = _det_uuid("book", i)
        title = f"Ksiazka {i:07d}"
        author = f"{FIRST_NAMES[i % len(FIRST_NAMES)]} {SURNAMES[i % len(SURNAMES)]}"
        publisher = PUBLISHERS[(i - 1) % len(PUBLISHERS)]
        publish_date = base_date - timedelta(days=(i % 3650))
        pages = 120 + (i % 700)
        in_room = "true" if i % 10 < 3 else "false"

        lines.append(
            "INSERT INTO books_by_shop ("
            "shop_id, book_id, author, title, publisher, publish_date, pages, is_in_reading_room"
            ") VALUES ("
            f"{shop_id}, {book_id}, '{_q(author)}', '{_q(title)}', '{_q(publisher)}', "
            f"'{publish_date.isoformat()}', {pages}, {in_room}"
            ");"
        )
        books.append((shop_id, book_id, title))
    return books


def _generate_rentals(
    lines: list[str],
    rental_count: int,
    user_ids: list[uuid.UUID],
    employee_ids: list[uuid.UUID],
    books: list[tuple[uuid.UUID, uuid.UUID, str]],
    base_date: date,
) -> None:
    for i in range(1, rental_count + 1):
        rental_id = _det_uuid("rental", i)
        user_id = user_ids[(i - 1) % len(user_ids)]
        employee_id = employee_ids[(i - 1) % len(employee_ids)]
        shop_id, book_id, _ = books[(i - 1) % len(books)]
        start_date = base_date - timedelta(days=(i % 730))
        is_returned = "true" if i % 4 != 0 else "false"
        end_date = f"'{(start_date + timedelta(days=14)).isoformat()}'" if is_returned == "true" else "null"
        rental_method = RENTAL_METHODS[(i - 1) % len(RENTAL_METHODS)]

        lines.append(
            "INSERT INTO rentals_by_user ("
            "user_id, start_date, rental_id, book_id, book_title, shop_id, employee_id, "
            "is_returned, end_date, rental_method"
            ") VALUES ("
            f"{user_id}, '{start_date.isoformat()}', {rental_id}, {book_id}, 'Ksiazka {(i - 1) % len(books) + 1:07d}', "
            f"{shop_id}, {employee_id}, {is_returned}, {end_date}, '{rental_method}'"
            ");"
        )

        lines.append(
            "INSERT INTO rentals_by_shop ("
            "shop_id, start_date, rental_id, book_id, user_id, employee_id, "
            "is_returned, end_date, rental_method"
            ") VALUES ("
            f"{shop_id}, '{start_date.isoformat()}', {rental_id}, {book_id}, {user_id}, {employee_id}, "
            f"{is_returned}, {end_date}, '{rental_method}'"
            ");"
        )


def _generate_reservations(
    lines: list[str],
    reservation_count: int,
    user_ids: list[uuid.UUID],
    books: list[tuple[uuid.UUID, uuid.UUID, str]],
    base_date: date,
) -> None:
    for i in range(1, reservation_count + 1):
        reservation_id = _det_uuid("reservation", i)
        user_id = user_ids[(i - 1) % len(user_ids)]
        _, book_id, book_title = books[(i - 1) % len(books)]
        when_reserved = base_date - timedelta(days=(i % 365))

        lines.append(
            "INSERT INTO reservations_by_user ("
            "user_id, when_reserved, reservation_id, book_id, book_title"
            ") VALUES ("
            f"{user_id}, '{when_reserved.isoformat()}', {reservation_id}, {book_id}, '{_q(book_title)}'"
            ");"
        )


def _generate_widecolumn_cql(
    engine_name: str,
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    counts = _resolve_counts(dataset_size, table_row_overrides)

    base_date = date(2026, 1, 1)
    lines: list[str] = [
        f"-- Generated inserts for {engine_name}",
        f"-- dataset_size={dataset_size}",
        "USE bench_bookshop;",
        "",
        "-- Stage 1: bookshops",
    ]

    shop_ids = [_det_uuid("shop", i) for i in range(1, BOOKSHOP_COUNT + 1)]
    _generate_bookshops(lines, shop_ids)

    lines.extend(["", "-- Stage 2: users and credentials"])
    user_ids = _generate_users(lines, counts["users"], shop_ids)

    lines.extend(["", "-- Stage 3: employees"])
    employee_ids = _generate_employees(lines, counts["employees_by_shop"], shop_ids, base_date)

    lines.extend(["", "-- Stage 4: books by shop"])
    books = _generate_books(lines, counts["books_by_shop"], shop_ids, base_date)

    lines.extend(["", "-- Stage 5: rentals by user and by shop"])
    _generate_rentals(lines, counts["rentals"], user_ids, employee_ids, books, base_date)

    lines.extend(["", "-- Stage 6: reservations by user"])
    _generate_reservations(lines, counts["reservations_by_user"], user_ids, books, base_date)

    return "\n".join(lines) + "\n"


def generate_cassandra_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    return _generate_widecolumn_cql("cassandra", dataset_size, table_row_overrides)


def generate_scylla_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    return _generate_widecolumn_cql("scylla", dataset_size, table_row_overrides)
