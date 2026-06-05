"""Generatory danych dla silnikow NoSQL (Cassandra, ScyllaDB)."""

from __future__ import annotations

import csv
from bisect import bisect_left
from datetime import date, timedelta
from pathlib import Path
from typing import Iterator
from uuid import NAMESPACE_DNS, uuid5


BOOK_SHOPS = [
    ("Book Haven", "ul. Dluga 12, Warszawa", "shop1@bench.local"),
    ("Readers Paradise", "ul. Lipowa 8, Krakow", "shop2@bench.local"),
    ("The Book Nook", "ul. Ogrodowa 19, Gdansk", "shop3@bench.local"),
    ("Page Turners", "ul. Sosnowa 4, Wroclaw", "shop4@bench.local"),
    ("Literary Lounge", "ul. Klonowa 21, Poznan", "shop5@bench.local"),
    ("Weekend Reads", "ul. Morska 15, Gdynia", "shop6@bench.local"),
    ("Silent Shelf", "ul. Kwiatowa 3, Lublin", "shop7@bench.local"),
    ("Open Chapter", "ul. Rynek 7, Szczecin", "shop8@bench.local"),
    ("Amber Pages", "ul. Sloneczna 18, Torun", "shop9@bench.local"),
    ("Midnight Stories", "ul. Wiosenna 25, Katowice", "shop10@bench.local"),
]

FIRST_NAMES = [
    "Piotr", "Anna", "Krzysztof", "Maria", "Andrzej",
    "Katarzyna", "Tomasz", "Malgorzata", "Pawel", "Agnieszka",
    "Jan", "Barbara", "Michal", "Ewa", "Marcin",
    "Magdalena", "Jakub", "Elzbieta", "Adam", "Joanna",
]

SURNAMES = [
    "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk",
    "Kaminski", "Lewandowski", "Zielinski", "Szymanski", "Wozniak",
    "Dabrowski", "Kozlowski", "Mazur", "Jankowski", "Kwiatkowski",
    "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski", "Zajac",
]

USER_STATUSES = ["ACTIVE", "INACTIVE", "PENDING", "SUSPENDED"]
USER_PERMISSIONS = [
    "WYPOZYCZENIE",
    "REZERWACJA_I_WYPOZYCZENIE",
    "TYLKO_REZERWACJA",
    "PELNY_DOSTEP",
]
EMPLOYEE_ROLES = ["Kierownik", "Ksiegarz", "Doradca klienta", "Magazynier"]
PUBLISHERS = ["PWN", "Znak", "Czarne", "Agora", "Marginesy"]
RENTAL_METHODS = ["Książkomat", "Wypożyczalnia"]


def _stable_uuid(prefix: str, row_id: int) -> str:
    return str(uuid5(NAMESPACE_DNS, f"bench:{prefix}:{row_id}"))


def _cql_quote(value: str) -> str:
    return value.replace("'", "''")


def _iso_date(base_date: date, offset_days: int) -> str:
    return (base_date + timedelta(days=offset_days)).isoformat()


def _resolve_row_counts(dataset_size: int) -> dict[str, int]:
    shop_count = len(BOOK_SHOPS)
    user_count = max(shop_count * 10, dataset_size // 10)
    employee_count = max(shop_count * 4, dataset_size // 12)
    book_count = max(shop_count * 20, dataset_size // 5)
    rental_count = max(shop_count * 10, dataset_size // 4)
    reservation_count = max(shop_count * 5, dataset_size // 10)

    return {
        "shops": shop_count,
        "users": user_count,
        "employees": employee_count,
        "books": book_count,
        "rentals": rental_count,
        "reservations": reservation_count,
    }


def _active_user_pool(users: list[dict[str, str]], activity_ratio: float = 0.2) -> list[dict[str, str]]:
    """Zwraca aktywna pule użytkowników dla nierównego rozkładu operacji."""
    active_count = max(1, min(len(users), int(len(users) * activity_ratio)))
    return users[:active_count]


def _build_user_weight_index(users: list[dict[str, str]]) -> tuple[list[int], int]:
    """Buduje skumulowane wagi, aby zwiększyć różnorodność liczby operacji na użytkownika."""
    if not users:
        return [1], 1

    weighted_prefix: list[int] = []
    total_weight = 0
    size = len(users)

    for idx, _ in enumerate(users):
        percentile = (idx + 1) / size
        if percentile <= 0.05:
            weight = 20
        elif percentile <= 0.20:
            weight = 8
        elif percentile <= 0.50:
            weight = 3
        else:
            weight = 1

        total_weight += weight
        weighted_prefix.append(total_weight)

    return weighted_prefix, total_weight


def _pick_weighted_user(
    users: list[dict[str, str]],
    weighted_prefix: list[int],
    total_weight: int,
    sequence: int,
    salt: int,
) -> dict[str, str]:
    """Deterministycznie wybiera użytkownika według rozkładu wag."""
    if len(users) == 1:
        return users[0]

    # Deterministyczny "pseudo-random" bez globalnego stanu RNG.
    mixed = (sequence * 1103515245 + 12345 + salt * 1000003) & 0x7FFFFFFF
    target = (mixed % total_weight) + 1
    chosen_idx = bisect_left(weighted_prefix, target)
    return users[chosen_idx]


def _build_book_weight_index(books: list[dict[str, str | int | bool]]) -> tuple[list[int], int]:
    """Buduje skumulowane wagi popularności książek (część tytułów wypożyczana częściej)."""
    if not books:
        return [1], 1

    weighted_prefix: list[int] = []
    total_weight = 0
    size = len(books)

    for idx, _ in enumerate(books):
        percentile = (idx + 1) / size
        if percentile <= 0.10:
            weight = 14
        elif percentile <= 0.35:
            weight = 5
        elif percentile <= 0.70:
            weight = 2
        else:
            weight = 1

        total_weight += weight
        weighted_prefix.append(total_weight)

    return weighted_prefix, total_weight


def _pick_weighted_book(
    books: list[dict[str, str | int | bool]],
    weighted_prefix: list[int],
    total_weight: int,
    sequence: int,
    salt: int,
) -> dict[str, str | int | bool]:
    """Deterministycznie wybiera książkę według rozkładu popularności."""
    if len(books) == 1:
        return books[0]

    mixed = (sequence * 214013 + 2531011 + salt * 1000033) & 0x7FFFFFFF
    target = (mixed % total_weight) + 1
    chosen_idx = bisect_left(weighted_prefix, target)
    return books[chosen_idx]


def _build_nosql_dataset(dataset_size: int) -> dict[str, object]:
    counts = _resolve_row_counts(dataset_size)
    shop_ids = [_stable_uuid("shop", idx + 1) for idx in range(counts["shops"])]

    employees: list[dict[str, str]] = []
    employees_by_shop: dict[str, list[dict[str, str]]] = {shop_id: [] for shop_id in shop_ids}
    manager_ids: dict[str, str] = {}
    employee_base_date = date(1980, 1, 1)

    for idx in range(counts["employees"]):
        row_id = idx + 1
        shop_index = idx % counts["shops"]
        shop_id = shop_ids[shop_index]
        first_name = FIRST_NAMES[idx % len(FIRST_NAMES)]
        surname = SURNAMES[(idx * 7) % len(SURNAMES)]
        employee_id = _stable_uuid("employee", row_id)
        role = EMPLOYEE_ROLES[0] if shop_id not in manager_ids else EMPLOYEE_ROLES[(idx % (len(EMPLOYEE_ROLES) - 1)) + 1]
        employee = {
            "primary_book_shop_id": shop_id,
            "employee_id": employee_id,
            "name": first_name,
            "surname": surname,
            "phone_number": f"+486{row_id:08d}",
            "email": f"emp.{first_name.lower()}.{surname.lower()}{row_id}@bench.local",
            "birth_date": _iso_date(employee_base_date, (idx * 17) % 9000),
            "started_at": _iso_date(date(2020, 1, 1), idx % 1800),
            "primary_business_role": role,
        }
        employees.append(employee)
        employees_by_shop[shop_id].append(employee)
        if shop_id not in manager_ids:
            manager_ids[shop_id] = employee_id

    users: list[dict[str, str]] = []
    for idx in range(counts["users"]):
        row_id = idx + 1
        shop_id = shop_ids[idx % counts["shops"]]
        per_shop_sequence = row_id // counts["shops"]
        first_name = FIRST_NAMES[(idx + per_shop_sequence * 7) % len(FIRST_NAMES)]
        surname = SURNAMES[((idx * 5) + (per_shop_sequence * 11)) % len(SURNAMES)]
        status = USER_STATUSES[idx % len(USER_STATUSES)]
        permission = USER_PERMISSIONS[idx % len(USER_PERMISSIONS)]
        user_id = _stable_uuid("user", row_id)
        users.append(
            {
                "user_id": user_id,
                "name": first_name,
                "surname": surname,
                "phone_number": f"+485{row_id:08d}",
                "email": f"{first_name.lower()}.{surname.lower()}{row_id}@bench.local",
                "main_book_shop_id": shop_id,
                "card_id_number": f"CARD-{row_id:09d}",
                "status": status,
                "login": f"user{row_id:07d}",
                "permissions": permission,
                "password_hash": f"hash_{_stable_uuid('password', row_id)}",
            }
        )

    books: list[dict[str, str | int | bool]] = []
    books_by_shop: dict[str, list[dict[str, str | int | bool]]] = {shop_id: [] for shop_id in shop_ids}
    for idx in range(counts["books"]):
        row_id = idx + 1
        shop_id = shop_ids[idx % counts["shops"]]
        first_name = FIRST_NAMES[(idx + 3) % len(FIRST_NAMES)]
        surname = SURNAMES[(idx * 3) % len(SURNAMES)]
        book = {
            "shop_id": shop_id,
            "book_id": _stable_uuid("book", row_id),
            "author": f"{first_name} {surname}",
            "title": f"Ksiazka {row_id:07d}",
            "publisher": PUBLISHERS[idx % len(PUBLISHERS)],
            "publish_date": _iso_date(date(2021, 1, 1), idx % 1500),
            "pages": 120 + (idx % 380),
            "is_in_reading_room": "true" if idx % 3 == 0 else "false",
        }
        books.append(book)
        books_by_shop[shop_id].append(book)

    rental_users = _active_user_pool(users, activity_ratio=0.35)
    reservation_users = _active_user_pool(users, activity_ratio=0.45)
    rental_prefix, rental_total_weight = _build_user_weight_index(rental_users)
    reservation_prefix, reservation_total_weight = _build_user_weight_index(reservation_users)
    shop_book_weight_index: dict[str, tuple[list[int], int]] = {
        shop_id: _build_book_weight_index(shop_books)
        for shop_id, shop_books in books_by_shop.items()
    }

    return {
        "counts": counts,
        "shop_ids": shop_ids,
        "employees": employees,
        "employees_by_shop": employees_by_shop,
        "manager_ids": manager_ids,
        "users": users,
        "books": books,
        "books_by_shop": books_by_shop,
        "rental_users": rental_users,
        "reservation_users": reservation_users,
        "rental_prefix": rental_prefix,
        "rental_total_weight": rental_total_weight,
        "reservation_prefix": reservation_prefix,
        "reservation_total_weight": reservation_total_weight,
        "shop_book_weight_index": shop_book_weight_index,
    }


def _write_csv_rows(csv_path: Path, headers: list[str], rows: list[list[object]]) -> None:
    with csv_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(headers)
        writer.writerows(rows)


def _count_for_shop(total_count: int, shops_count: int, shop_index: int) -> int:
    """Zwraca liczbe rekordow przypadajaca na dany sklep przy podziale round-robin."""
    base = total_count // shops_count
    extra = 1 if shop_index < (total_count % shops_count) else 0
    return base + extra


def _row_id_for_shop(shop_index: int, local_index: int, shops_count: int) -> int:
    """Mapuje indeks lokalny (w sklepie) na globalny row_id przy rozkladzie round-robin."""
    return shop_index + 1 + local_index * shops_count


def _book_fields_from_row_id(row_id: int) -> dict[str, str | int]:
    idx = row_id - 1
    first_name = FIRST_NAMES[(idx + 3) % len(FIRST_NAMES)]
    surname = SURNAMES[(idx * 3) % len(SURNAMES)]
    return {
        "book_id": _stable_uuid("book", row_id),
        "author": f"{first_name} {surname}",
        "title": f"Ksiazka {row_id:07d}",
        "publisher": PUBLISHERS[idx % len(PUBLISHERS)],
        "publish_date": _iso_date(date(2021, 1, 1), idx % 1500),
        "pages": 120 + (idx % 380),
        "is_in_reading_room": "true" if idx % 3 == 0 else "false",
    }


def _stream_nosql_bulk_csv_files(output_path: Path, dataset_size: int) -> None:
    """Generuje CSV strumieniowo, bez budowania duzych list w RAM."""
    counts = _resolve_row_counts(dataset_size)
    shops_count = counts["shops"]
    shop_ids = [_stable_uuid("shop", idx + 1) for idx in range(shops_count)]

    manager_ids = {
        shop_id: _stable_uuid("employee", shop_index + 1)
        for shop_index, shop_id in enumerate(shop_ids)
    }

    with (output_path / "bookshops.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow([
            "shop_id", "shop_name", "address", "email", "manager_id",
            "opens_at_monday", "closes_at_monday", "opens_at_tuesday", "closes_at_tuesday",
            "opens_at_wednesday", "closes_at_wednesday", "opens_at_thursday", "closes_at_thursday",
            "opens_at_friday", "closes_at_friday", "opens_at_saturday", "closes_at_saturday",
            "opens_at_sunday", "closes_at_sunday",
        ])
        for idx, (shop_name, address, email) in enumerate(BOOK_SHOPS):
            shop_id = shop_ids[idx]
            writer.writerow([
                shop_id,
                shop_name,
                address,
                email,
                manager_ids[shop_id],
                "08:00:00",
                "20:00:00",
                "08:00:00",
                "20:00:00",
                "08:00:00",
                "20:00:00",
                "08:00:00",
                "20:00:00",
                "08:00:00",
                "20:00:00",
                "09:00:00",
                "18:00:00",
                "10:00:00",
                "16:00:00",
            ])

    with (output_path / "users.csv").open("w", encoding="utf-8", newline="") as users_handle, \
         (output_path / "user_credentials_by_login.csv").open("w", encoding="utf-8", newline="") as creds_handle:
        users_writer = csv.writer(users_handle)
        users_writer.writerow([
            "user_id", "name", "surname", "phone_number", "email", "main_book_shop_id",
            "card_id_number", "status", "login", "permissions",
        ])

        creds_writer = csv.writer(creds_handle)
        creds_writer.writerow(["login", "user_id", "password_hash", "status"])

        for idx in range(counts["users"]):
            row_id = idx + 1
            shop_id = shop_ids[idx % shops_count]
            per_shop_sequence = row_id // shops_count
            first_name = FIRST_NAMES[(idx + per_shop_sequence * 7) % len(FIRST_NAMES)]
            surname = SURNAMES[((idx * 5) + (per_shop_sequence * 11)) % len(SURNAMES)]
            status = USER_STATUSES[idx % len(USER_STATUSES)]
            permission = USER_PERMISSIONS[idx % len(USER_PERMISSIONS)]
            user_id = _stable_uuid("user", row_id)
            login = f"user{row_id:07d}"
            password_hash = f"hash_{_stable_uuid('password', row_id)}"

            users_writer.writerow([
                user_id,
                first_name,
                surname,
                f"+485{row_id:08d}",
                f"{first_name.lower()}.{surname.lower()}{row_id}@bench.local",
                shop_id,
                f"CARD-{row_id:09d}",
                status,
                login,
                f"{{'{permission}'}}",
            ])

            creds_writer.writerow([
                login,
                user_id,
                password_hash,
                status,
            ])

    with (output_path / "employees_by_shop.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow([
            "primary_book_shop_id", "employee_id", "name", "surname", "phone_number", "email",
            "birth_date", "started_at", "primary_business_role",
        ])

        employee_base_date = date(1980, 1, 1)
        manager_seen: set[str] = set()
        for idx in range(counts["employees"]):
            row_id = idx + 1
            shop_index = idx % shops_count
            shop_id = shop_ids[shop_index]
            first_name = FIRST_NAMES[idx % len(FIRST_NAMES)]
            surname = SURNAMES[(idx * 7) % len(SURNAMES)]
            employee_id = _stable_uuid("employee", row_id)
            role = EMPLOYEE_ROLES[0] if shop_id not in manager_seen else EMPLOYEE_ROLES[(idx % (len(EMPLOYEE_ROLES) - 1)) + 1]
            manager_seen.add(shop_id)

            writer.writerow([
                shop_id,
                employee_id,
                first_name,
                surname,
                f"+486{row_id:08d}",
                f"emp.{first_name.lower()}.{surname.lower()}{row_id}@bench.local",
                _iso_date(employee_base_date, (idx * 17) % 9000),
                _iso_date(date(2020, 1, 1), idx % 1800),
                role,
            ])

    with (output_path / "books_by_shop.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["shop_id", "book_id", "author", "title", "publisher", "publish_date", "pages", "is_in_reading_room"])
        for idx in range(counts["books"]):
            row_id = idx + 1
            shop_id = shop_ids[idx % shops_count]
            book = _book_fields_from_row_id(row_id)
            writer.writerow([
                shop_id,
                book["book_id"],
                book["author"],
                book["title"],
                book["publisher"],
                book["publish_date"],
                book["pages"],
                book["is_in_reading_room"],
            ])

    with (output_path / "rentals_by_user.csv").open("w", encoding="utf-8", newline="") as user_handle, \
         (output_path / "rentals_by_shop.csv").open("w", encoding="utf-8", newline="") as shop_handle:
        user_writer = csv.writer(user_handle)
        user_writer.writerow([
            "user_id", "start_date", "rental_id", "book_id", "book_title", "shop_id",
            "employee_id", "is_returned", "end_date", "rental_method",
        ])

        shop_writer = csv.writer(shop_handle)
        shop_writer.writerow([
            "shop_id", "start_date", "rental_id", "book_id", "user_id", "employee_id",
            "is_returned", "end_date", "rental_method",
        ])

        rental_active_users = max(1, int(counts["users"] * 0.35))
        for idx in range(counts["rentals"]):
            row_id = idx + 1
            user_row_id = ((row_id * 7 - 1) % rental_active_users) + 1
            user_id = _stable_uuid("user", user_row_id)

            shop_index = idx % shops_count
            shop_id = shop_ids[shop_index]

            books_in_shop = _count_for_shop(counts["books"], shops_count, shop_index)
            if books_in_shop <= 0:
                continue
            book_local_index = (row_id * 11 - 1) % books_in_shop
            book_row_id = _row_id_for_shop(shop_index, book_local_index, shops_count)
            book = _book_fields_from_row_id(book_row_id)

            employees_in_shop = _count_for_shop(counts["employees"], shops_count, shop_index)
            if employees_in_shop <= 0:
                continue
            employee_local_index = idx % employees_in_shop
            employee_row_id = _row_id_for_shop(shop_index, employee_local_index, shops_count)
            employee_id = _stable_uuid("employee", employee_row_id)

            start_date = _iso_date(date(2024, 1, 1), idx % 365)
            end_date = _iso_date(date(2024, 1, 8), idx % 365)
            rental_id = _stable_uuid("rental", row_id)
            rental_method = RENTAL_METHODS[idx % len(RENTAL_METHODS)]
            is_returned = "true" if idx % 4 != 0 else "false"

            user_writer.writerow([
                user_id,
                start_date,
                rental_id,
                book["book_id"],
                book["title"],
                shop_id,
                employee_id,
                is_returned,
                end_date,
                rental_method,
            ])

            shop_writer.writerow([
                shop_id,
                start_date,
                rental_id,
                book["book_id"],
                user_id,
                employee_id,
                is_returned,
                end_date,
                rental_method,
            ])

    with (output_path / "reservations_by_user.csv").open("w", encoding="utf-8", newline="") as handle:
        writer = csv.writer(handle)
        writer.writerow(["user_id", "when_reserved", "reservation_id", "book_id", "book_title"])

        reservation_active_users = max(1, int(counts["users"] * 0.45))
        for idx in range(counts["reservations"]):
            row_id = idx + 1
            user_row_id = ((row_id * 9 - 1) % reservation_active_users) + 1
            user_id = _stable_uuid("user", user_row_id)
            book_row_id = ((idx * 5) % counts["books"]) + 1
            book = _book_fields_from_row_id(book_row_id)
            when_reserved = _iso_date(date(2024, 6, 1), idx % 240)
            reservation_id = _stable_uuid("reservation", row_id)

            writer.writerow([
                user_id,
                when_reserved,
                reservation_id,
                book["book_id"],
                book["title"],
            ])


def write_nosql_bulk_csv_files(output_dir: str | Path, dataset_size: int) -> None:
    """Zapisuje CSV do bulk importu dla Cassandra i ScyllaDB."""
    output_path = Path(output_dir)
    output_path.mkdir(parents=True, exist_ok=True)
    _stream_nosql_bulk_csv_files(output_path, dataset_size)


def _generate_nosql_copy_script(
    engine_name: str,
    import_dir: str = "/tmp/bench_bulk",
    copy_options: str = "HEADER = true AND NULL = ''",
) -> str:
    lines = [
        "CONSISTENCY LOCAL_ONE;",
        "",
        f"-- Bulk COPY script for {engine_name}",
        f"COPY bench.bookshops (shop_id, shop_name, address, email, manager_id, opens_at_monday, closes_at_monday, opens_at_tuesday, closes_at_tuesday, opens_at_wednesday, closes_at_wednesday, opens_at_thursday, closes_at_thursday, opens_at_friday, closes_at_friday, opens_at_saturday, closes_at_saturday, opens_at_sunday, closes_at_sunday) FROM '{import_dir}/bookshops.csv' WITH {copy_options};",
        f"COPY bench.books_by_shop (shop_id, book_id, author, title, publisher, publish_date, pages, is_in_reading_room) FROM '{import_dir}/books_by_shop.csv' WITH {copy_options};",
        f"COPY bench.users (user_id, name, surname, phone_number, email, main_book_shop_id, card_id_number, status, login, permissions) FROM '{import_dir}/users.csv' WITH {copy_options};",
        f"COPY bench.user_credentials_by_login (login, user_id, password_hash, status) FROM '{import_dir}/user_credentials_by_login.csv' WITH {copy_options};",
        f"COPY bench.employees_by_shop (primary_book_shop_id, employee_id, name, surname, phone_number, email, birth_date, started_at, primary_business_role) FROM '{import_dir}/employees_by_shop.csv' WITH {copy_options};",
        f"COPY bench.rentals_by_user (user_id, start_date, rental_id, book_id, book_title, shop_id, employee_id, is_returned, end_date, rental_method) FROM '{import_dir}/rentals_by_user.csv' WITH {copy_options};",
        f"COPY bench.rentals_by_shop (shop_id, start_date, rental_id, book_id, user_id, employee_id, is_returned, end_date, rental_method) FROM '{import_dir}/rentals_by_shop.csv' WITH {copy_options};",
        f"COPY bench.reservations_by_user (user_id, when_reserved, reservation_id, book_id, book_title) FROM '{import_dir}/reservations_by_user.csv' WITH {copy_options};",
        "",
    ]
    return "\n".join(lines)


def generate_cassandra_copy_script(dataset_size: int, import_dir: str = "/tmp/bench_bulk") -> str:
    """Generuje skrypt COPY dla Cassandry."""
    _ = dataset_size
    return _generate_nosql_copy_script("cassandra", import_dir)


def generate_scylla_copy_script(dataset_size: int, import_dir: str = "/tmp/bench_bulk") -> str:
    """Generuje skrypt COPY dla ScyllaDB."""
    _ = dataset_size
    scylla_copy_options = (
        "HEADER = true AND NULL = '' "
        "AND NUMPROCESSES = 1 "
        "AND CHUNKSIZE = 200 "
        "AND MAXBATCHSIZE = 8 "
        "AND MAXATTEMPTS = 25 "
        "AND INGESTRATE = 5000"
    )
    return _generate_nosql_copy_script("scylla", import_dir, copy_options=scylla_copy_options)


def _iter_cql_lines(engine_name: str, dataset_size: int) -> Iterator[str]:
    counts = _resolve_row_counts(dataset_size)
    shops_count = counts["shops"]
    shop_ids = [_stable_uuid("shop", idx + 1) for idx in range(shops_count)]

    manager_ids = {
        shop_id: _stable_uuid("employee", shop_index + 1)
        for shop_index, shop_id in enumerate(shop_ids)
    }

    yield f"-- Generated inserts for {engine_name}"
    yield f"-- dataset_size={dataset_size}"
    yield "USE bench;"
    yield ""
    yield "-- Stage 1: bookshops"

    for idx, (shop_name, address, email) in enumerate(BOOK_SHOPS):
        shop_id = shop_ids[idx]
        yield (
            "INSERT INTO bookshops "
            "(shop_id, shop_name, address, email, manager_id, opens_at_monday, closes_at_monday, "
            "opens_at_tuesday, closes_at_tuesday, opens_at_wednesday, closes_at_wednesday, "
            "opens_at_thursday, closes_at_thursday, opens_at_friday, closes_at_friday, "
            "opens_at_saturday, closes_at_saturday, opens_at_sunday, closes_at_sunday) VALUES "
            f"({shop_id}, '{_cql_quote(shop_name)}', '{_cql_quote(address)}', '{_cql_quote(email)}', "
            f"{manager_ids[shop_id]}, '08:00:00', '20:00:00', '08:00:00', '20:00:00', '08:00:00', '20:00:00', "
            "'08:00:00', '20:00:00', '08:00:00', '20:00:00', '09:00:00', '18:00:00', '10:00:00', '16:00:00');"
        )

    yield ""
    yield "-- Stage 2: users and credentials"
    for idx in range(counts["users"]):
        row_id = idx + 1
        shop_id = shop_ids[idx % shops_count]
        per_shop_sequence = row_id // shops_count
        first_name = FIRST_NAMES[(idx + per_shop_sequence * 7) % len(FIRST_NAMES)]
        surname = SURNAMES[((idx * 5) + (per_shop_sequence * 11)) % len(SURNAMES)]
        status = USER_STATUSES[idx % len(USER_STATUSES)]
        permission = USER_PERMISSIONS[idx % len(USER_PERMISSIONS)]
        user_id = _stable_uuid("user", row_id)
        login = f"user{row_id:07d}"
        password_hash = f"hash_{_stable_uuid('password', row_id)}"

        yield (
            "INSERT INTO users "
            "(user_id, name, surname, phone_number, email, main_book_shop_id, card_id_number, status, login, permissions) VALUES "
            f"({user_id}, '{_cql_quote(first_name)}', '{_cql_quote(surname)}', "
            f"'+485{row_id:08d}', '{first_name.lower()}.{surname.lower()}{row_id}@bench.local', "
            f"{shop_id}, 'CARD-{row_id:09d}', '{status}', '{login}', {{'{permission}'}});"
        )
        yield (
            "INSERT INTO user_credentials_by_login "
            "(login, user_id, password_hash, status) VALUES "
            f"('{login}', {user_id}, '{password_hash}', '{status}');"
        )

    yield ""
    yield "-- Stage 3: employees"
    manager_seen: set[str] = set()
    for idx in range(counts["employees"]):
        row_id = idx + 1
        shop_index = idx % shops_count
        shop_id = shop_ids[shop_index]
        first_name = FIRST_NAMES[idx % len(FIRST_NAMES)]
        surname = SURNAMES[(idx * 7) % len(SURNAMES)]
        role = EMPLOYEE_ROLES[0] if shop_id not in manager_seen else EMPLOYEE_ROLES[(idx % (len(EMPLOYEE_ROLES) - 1)) + 1]
        manager_seen.add(shop_id)

        yield (
            "INSERT INTO employees_by_shop "
            "(primary_book_shop_id, employee_id, name, surname, phone_number, email, birth_date, started_at, primary_business_role) VALUES "
            f"({shop_id}, {_stable_uuid('employee', row_id)}, '{_cql_quote(first_name)}', '{_cql_quote(surname)}', "
            f"'+486{row_id:08d}', 'emp.{first_name.lower()}.{surname.lower()}{row_id}@bench.local', "
            f"'{_iso_date(date(1980, 1, 1), (idx * 17) % 9000)}', '{_iso_date(date(2020, 1, 1), idx % 1800)}', '{role}');"
        )

    yield ""
    yield "-- Stage 4: books by shop"
    for idx in range(counts["books"]):
        row_id = idx + 1
        shop_id = shop_ids[idx % shops_count]
        book = _book_fields_from_row_id(row_id)
        yield (
            "INSERT INTO books_by_shop "
            "(shop_id, book_id, author, title, publisher, publish_date, pages, is_in_reading_room) VALUES "
            f"({shop_id}, {book['book_id']}, '{_cql_quote(str(book['author']))}', '{_cql_quote(str(book['title']))}', "
            f"'{_cql_quote(str(book['publisher']))}', '{book['publish_date']}', {book['pages']}, {book['is_in_reading_room']});"
        )

    yield ""
    yield "-- Stage 5: rentals by user and by shop"
    rental_active_users = max(1, int(counts["users"] * 0.35))
    for idx in range(counts["rentals"]):
        row_id = idx + 1
        user_row_id = ((row_id * 7 - 1) % rental_active_users) + 1
        user_id = _stable_uuid("user", user_row_id)

        shop_index = idx % shops_count
        shop_id = shop_ids[shop_index]

        books_in_shop = _count_for_shop(counts["books"], shops_count, shop_index)
        if books_in_shop <= 0:
            continue
        book_local_index = (row_id * 11 - 1) % books_in_shop
        book_row_id = _row_id_for_shop(shop_index, book_local_index, shops_count)
        book = _book_fields_from_row_id(book_row_id)

        employees_in_shop = _count_for_shop(counts["employees"], shops_count, shop_index)
        if employees_in_shop <= 0:
            continue
        employee_local_index = idx % employees_in_shop
        employee_row_id = _row_id_for_shop(shop_index, employee_local_index, shops_count)
        employee_id = _stable_uuid("employee", employee_row_id)

        start_date = _iso_date(date(2024, 1, 1), idx % 365)
        end_date = _iso_date(date(2024, 1, 8), idx % 365)
        rental_id = _stable_uuid("rental", row_id)
        rental_method = RENTAL_METHODS[idx % len(RENTAL_METHODS)]
        is_returned = "true" if idx % 4 != 0 else "false"

        yield (
            "INSERT INTO rentals_by_user "
            "(user_id, start_date, rental_id, book_id, book_title, shop_id, employee_id, is_returned, end_date, rental_method) VALUES "
            f"({user_id}, '{start_date}', {rental_id}, {book['book_id']}, '{_cql_quote(str(book['title']))}', "
            f"{shop_id}, {employee_id}, {is_returned}, '{end_date}', '{rental_method}');"
        )
        yield (
            "INSERT INTO rentals_by_shop "
            "(shop_id, start_date, rental_id, book_id, user_id, employee_id, is_returned, end_date, rental_method) VALUES "
            f"({shop_id}, '{start_date}', {rental_id}, {book['book_id']}, {user_id}, {employee_id}, "
            f"{is_returned}, '{end_date}', '{rental_method}');"
        )

    yield ""
    yield "-- Stage 6: reservations by user"
    reservation_active_users = max(1, int(counts["users"] * 0.45))
    for idx in range(counts["reservations"]):
        row_id = idx + 1
        user_row_id = ((row_id * 9 - 1) % reservation_active_users) + 1
        user_id = _stable_uuid("user", user_row_id)
        book_row_id = ((idx * 5) % counts["books"]) + 1
        book = _book_fields_from_row_id(book_row_id)
        when_reserved = _iso_date(date(2024, 6, 1), idx % 240)
        reservation_id = _stable_uuid("reservation", row_id)

        yield (
            "INSERT INTO reservations_by_user "
            "(user_id, when_reserved, reservation_id, book_id, book_title) VALUES "
            f"({user_id}, '{when_reserved}', {reservation_id}, {book['book_id']}, '{_cql_quote(str(book['title']))}');"
        )

    yield ""


def _write_cql_file(output_path: str | Path, engine_name: str, dataset_size: int) -> None:
    path = Path(output_path)
    with path.open("w", encoding="utf-8") as handle:
        for line in _iter_cql_lines(engine_name, dataset_size):
            handle.write(line)
            handle.write("\n")


def _build_cql(engine_name: str, dataset_size: int) -> str:
    return "\n".join(_iter_cql_lines(engine_name, dataset_size))


def generate_cassandra_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    """Generuje inserty CQL dla Cassandra."""
    _ = table_row_overrides
    return _build_cql("cassandra", dataset_size)


def write_cassandra_cql_file(
    output_path: str | Path,
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> None:
    """Zapisuje inserty CQL dla Cassandra bez alokacji ogromnego stringa."""
    _ = table_row_overrides
    _write_cql_file(output_path, "cassandra", dataset_size)


def generate_scylla_cql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> str:
    """Generuje inserty CQL dla ScyllaDB."""
    _ = table_row_overrides
    return _build_cql("scylla", dataset_size)


def write_scylla_cql_file(
    output_path: str | Path,
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
) -> None:
    """Zapisuje inserty CQL dla Scylla bez alokacji ogromnego stringa."""
    _ = table_row_overrides
    _write_cql_file(output_path, "scylla", dataset_size)
