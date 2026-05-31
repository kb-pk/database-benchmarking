
from __future__ import annotations
# Generator insertów do tabeli BookRental
import random
"""Generatory plikow SQL z insertami.

Ten modul na tym etapie generuje:
- tabele bez kluczy obcych,
- kolejne tabele zalezne zgodnie z bezpieczna kolejnoscia ladowania.

Wymaganie: PostgreSQL i MSSQL dostaja identyczna tresc SQL.
"""


RELATIONAL_TABLES = (
    "ActivationStatus",
    "UserAccountPermissions",
    "BookRentalMethod",
    "BookShopUser",
    "UserCard",
    "UserAccount",
    "Employee",
    "BookShop",
    "Book",
    "BookShopOffering",
    "BookReservation",
    "BookRental",
    "BookShopOpeningHours",
)

ACTIVATION_STATUSES = ["ACTIVE", "INACTIVE", "PENDING", "SUSPENDED", "ARCHIVED"]
BOOK_RENTAL_METHODS = ["Książkomat", "Wypożyczalnia"]
BOOK_SHOPS = [
    ("Book Haven", "ul. Dluga 12, Warszawa", "bookhaven@bench.local"),
    ("Readers Paradise", "ul. Lipowa 8, Krakow", "readersparadise@bench.local"),
    ("The Book Nook", "ul. Ogrodowa 19, Gdansk", "thebooknook@bench.local"),
    ("Page Turners", "ul. Sosnowa 4, Wroclaw", "pageturners@bench.local"),
    ("Literary Lounge", "ul. Klonowa 21, Poznan", "literarylounge@bench.local"),
    ("Weekend Reads", "ul. Morska 15, Gdynia", "weekendreads@bench.local"),
    ("Silent Shelf", "ul. Kwiatowa 3, Lublin", "silentshelf@bench.local"),
    ("Open Chapter", "ul. Rynek 7, Szczecin", "openchapter@bench.local"),
    ("Amber Pages", "ul. Sloneczna 18, Torun", "amberpages@bench.local"),
    ("Midnight Stories", "ul. Wiosenna 25, Katowice", "midnightstories@bench.local"),
]

# Tymczasowy globalny rozmiar danych dla tabel "duzych".
DEFAULT_ROW_COUNT = 500

# Prosta, bezposrednia konfiguracja liczby rekordow per tabela.
TABLE_ROW_COUNTS = {
    "ActivationStatus": len(ACTIVATION_STATUSES),
    "UserAccountPermissions": 5,
    "BookRentalMethod": len(BOOK_RENTAL_METHODS),
    "BookShopUser": DEFAULT_ROW_COUNT,
    "UserCard": DEFAULT_ROW_COUNT,
    "UserAccount": DEFAULT_ROW_COUNT,
    "Employee": len(BOOK_SHOPS),
    "BookShop": len(BOOK_SHOPS),
    "Book": 5000,
    "BookShopOffering": 2500,
    "BookReservation": 250,
    "BookRental": 350,
    "BookShopOpeningHours": len(BOOK_SHOPS),
}


def _pick_user_id_with_activity_skew(
    row_id: int,
    user_count: int,
    *,
    activity_ratio: float = 0.2,
    step: int = 7,
) -> int:
    """Zwraca user_id z lekkim skosem aktywności.

    Założenie: tylko część użytkowników jest bardzo aktywna,
    więc pojawiają się wielokrotne rezerwacje/wypożyczenia dla tych samych osób.
    """
    active_user_count = max(1, min(user_count, int(user_count * activity_ratio)))
    return ((row_id * step - 1) % active_user_count) + 1


def _sql_quote(value: str) -> str:
    """Escapuje apostrofy do bezpiecznego literału SQL."""
    return value.replace("'", "''")


def calculate_table_row_counts(total_rows: int, min_users: int = 20, max_users: int = 5000) -> dict[str, int]:
    """Inteligentnie wylicza liczbę rekordów dla każdej tabeli na bazie całkowitej liczby.
    
    Dane stałe (niezmienialne):
    - ActivationStatus: 5
    - UserAccountPermissions: 5
    - BookRentalMethod: 2
    - BookShop: 10
    - Employee: 100 (ale wielokrotnie na sklep)
    
    Pozostale dane dziela sie 20/80:
    - 20% na uzytkownikow (BookShopUser, UserCard, UserAccount)
    - 80% na ksiazki/wypozyczenia (Book, BookShopOffering, BookReservation, BookRental)
    
    Zależności 1:1 automatycznie synchronizowane:
    - UserCard zawsze = BookShopUser
    - UserAccount zawsze = BookShopUser
    - BookShopOpeningHours zawsze = BookShop
    """
    # Dane stałe
    const_counts = {
        "ActivationStatus": 5,
        "UserAccountPermissions": 5,
        "BookRentalMethod": 2,
        "BookShop": len(BOOK_SHOPS),  # 10
        "Employee": 100,
    }
    total_const = sum(const_counts.values())  # 122
    
    # Sprawdź czy możliwe
    if total_rows < total_const:
        raise ValueError(
            f"total_rows ({total_rows}) musi być >= {total_const} (dane stałe)"
        )
    
    # Pozostało do podziału
    remaining = total_rows - total_const
    
    # Podziel 20/80 (uzytkowicy/ksiazki)
    user_data_rows = remaining // 5
    book_data_rows = remaining - user_data_rows
    
    # Dla użytkowników: BookShopUser + UserCard (1:1) + UserAccount (1:1) = 3x
    # Podziel tako aby mieści się w zakresie 20-5000
    book_shop_user_count = max(min_users, min(max_users, user_data_rows // 3))
    user_card_count = book_shop_user_count  # 1:1 zależność
    user_account_count = book_shop_user_count  # 1:1 zależność
    
    # Dla ksiazek: Book + BookShopOffering + BookReservation + BookRental
    # Zwiększ udział książek do 60% book_data_rows, reszta na powiązania
    book_count = (book_data_rows * 60) // 100
    book_shop_offering_count = book_count  # 1:1 lub wiecej
    # Reszta na rezerwacje i wypozyczenia
    remaining_book_data = book_data_rows - book_count - book_shop_offering_count
    book_reservation_count = remaining_book_data // 2
    book_rental_count = remaining_book_data - book_reservation_count
    
    # BookShopOpeningHours zawsze = BookShop
    book_shop_opening_hours_count = const_counts["BookShop"]
    
    return {
        "ActivationStatus": const_counts["ActivationStatus"],
        "UserAccountPermissions": const_counts["UserAccountPermissions"],
        "BookRentalMethod": const_counts["BookRentalMethod"],
        "BookShop": const_counts["BookShop"],
        "Employee": const_counts["Employee"],
        "BookShopUser": book_shop_user_count,
        "UserCard": user_card_count,
        "UserAccount": user_account_count,
        "Book": book_count,
        "BookShopOffering": book_shop_offering_count,
        "BookReservation": book_reservation_count,
        "BookRental": book_rental_count,
        "BookShopOpeningHours": book_shop_opening_hours_count,
    }


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
    counts["BookShopOpeningHours"] = counts["BookShop"]
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

    mainBookShopId ustawiamy tymczasowo na NULL, a powiazanie dopinamy
    po wygenerowaniu bench.BookShop.
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
        is_active_id = (((row_id - 1) // len(BOOK_SHOPS)) % 5) + 1
        lines.append(
            "INSERT INTO bench.BookShopUser "
            "(id, name, surname, phoneNumber, email, mainBookShopId, isActiveId) "
            f"VALUES ({row_id}, '{_sql_quote(name)}', '{_sql_quote(surname)}', "
            f"'{_sql_quote(phone_number)}', '{_sql_quote(email)}', NULL, {is_active_id});"
        )
    return lines


def generate_user_card_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.UserCard.

    Karta uzytkownika: 1 karta na uzytkownika (1:1 relacja z BookShopUser).
    cardIdNumber: unikalna liczba karty w formacie standardowym.
    isActiveId: odwolanie do ActivationStatus (przedzial 1-5).
    userId: odwolanie do BookShopUser.
    """
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        # Card number: standardowy format: PPLL-NNNNNNNN-CCCC (PP=prefix, LL=library, NN=sequence)
        card_id_number = f"LIB-{row_id // 1000:02d}-{row_id % 1000:04d}"
        # Rotation przez statusy aktywnosci: wiekszy rozrzut niz taki sam status dla wszystkich
        is_active_id = ((row_id - 1) % 5) + 1
        # Zawsze mapuj 1:1 do uzytkownika
        user_id = row_id
        lines.append(
            "INSERT INTO bench.UserCard "
            "(id, cardIdNumber, isActiveId, userId) "
            f"VALUES ({row_id}, '{_sql_quote(card_id_number)}', {is_active_id}, {user_id});"
        )
    return lines


def generate_user_account_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.UserAccount.

    Konto uzytkownika: 1:1 relacja z BookShopUser.
    login: unikalny login pochodzacy od id uzytkownika
    passwordHash: symulowana hash hasla (SHA256-like format)
    permissionsId: odwolanie do UserAccountPermissions (przedzial 1-5)
    userId: odwolanie do BookShopUser (1:1)
    """
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        # Login: wzor user{id}
        login = f"user{row_id:06d}"
        # Password hash: symulowany SHA256 hash w formacie hex
        # Generujemy "losowy" hash na podstawie row_id
        password_hash = f"sha256${row_id:032x}$abcdef1234567890"
        # Rotation przez uprawnienia: lepszy rozrzut niz wszystkie FULL_ACCESS
        permissions_id = ((row_id - 1) % 5) + 1
        # 1:1 mapowanie do BookShopUser
        user_id = row_id
        lines.append(
            "INSERT INTO bench.UserAccount "
            "(id, login, passwordHash, permissionsId, userId) "
            f"VALUES ({row_id}, '{_sql_quote(login)}', '{_sql_quote(password_hash)}', {permissions_id}, {user_id});"
        )
    return lines


def generate_employee_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.Employee.

    primaryBookShopId ustawiamy tymczasowo na NULL, a po wygenerowaniu sklepow
    dopinamy relacje blokiem UPDATE.
    """
    first_names = [
        "Piotr", "Anna", "Krzysztof", "Maria", "Andrzej",
        "Katarzyna", "Tomasz", "Malgorzata", "Pawel", "Agnieszka",
        "Jan", "Barbara", "Michal", "Ewa", "Marcin",
        "Magdalena", "Jakub", "Elzbieta", "Adam", "Joanna",
        "Stanislav", "Izabela", "Grzegorz", "Urszula", "Wojciech",
    ]
    surnames = [
        "Nowak", "Kowalski", "Wisniewski", "Wojcik", "Kowalczyk",
        "Kaminski", "Lewandowski", "Zielinski", "Szymanski", "Wozniak",
        "Dabrowski", "Kozlowski", "Mazur", "Jankowski", "Kwiatkowski",
        "Krawczyk", "Kaczmarek", "Piotrowski", "Grabowski", "Zajac",
        "Pawlowski", "Michalski", "Krol", "Wieczorek", "Jablonski",
    ]
    roles = [
        "Kierownik ksiegarni",
        "Starszy bibliotekarz",
        "Specjalista obslugi klienta",
        "Koordynator zamowien",
        "Opiekun czytelni",
        "Administrator systemu",
        "Sprzątaczka",
        "Magazynier",
        "Ochroniarz",
        "Recepcjonista",
        "Ksiegowy",
        "Dyrektor finansowy",
    ]

    lines: list[str] = []
    book_shop_count = len(BOOK_SHOPS)
    for row_id in range(1, row_count + 1):
        name = first_names[(row_id - 1) % len(first_names)]
        surname = surnames[(row_id * 3 - 1) % len(surnames)]
        phone_number = f"+48{500000000 + row_id:09d}"
        email = f"pracownik.{name.lower()}.{surname.lower()}{row_id}@bench.local"
        birth_date = f"{1975 + (row_id % 20):04d}-{((row_id % 12) + 1):02d}-{((row_id % 28) + 1):02d}"
        started_at = f"{2015 + (row_id % 9):04d}-{((row_id % 12) + 1):02d}-01"
        primary_role = roles[(row_id - 1) % len(roles)]
        lines.append(
            "INSERT INTO bench.Employee "
            "(id, name, surname, phoneNumber, email, birthDate, startedAt, primaryBookShopId, primaryBusinessRole) "
            f"VALUES ({row_id}, '{_sql_quote(name)}', '{_sql_quote(surname)}', "
            f"'{_sql_quote(phone_number)}', '{_sql_quote(email)}', '{birth_date}', '{started_at}', "
            f"NULL, '{_sql_quote(primary_role)}');"
        )
    return lines


def generate_book_shop_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.BookShop.

    Tabela ma maksymalnie 10 rekordow inspirowanych danymi seed.
    managerId jest ustawiane 1:1 do id sklepu, a openingHoursId zostaje
    tymczasowo puste do czasu wygenerowania bench.BookShopOpeningHours.
    """
    max_book_shops = len(BOOK_SHOPS)
    if row_count > max_book_shops:
        raise ValueError(
            "bench.BookShop obsluguje maksymalnie 10 rekordow w obecnym szkielecie.",
        )

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        shop_name, address, email = BOOK_SHOPS[row_id - 1]
        manager_id = row_id
        lines.append(
            "INSERT INTO bench.BookShop "
            "(id, shopName, address, email, managerId, openingHoursId) "
            f"VALUES ({row_id}, '{_sql_quote(shop_name)}', '{_sql_quote(address)}', "
            f"'{_sql_quote(email)}', {manager_id}, NULL);"
        )
    return lines


def generate_book_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.Book.

    Ksiazki sa rozprowadzane miedzy sklepy (bookShopId oparty na modulo).
    """
    authors = [
        "Isaac Asimov", "Arthur C. Clarke", "Philip K. Dick", "Ursula K. Le Guin", 
        "Douglas Adams", "Ray Bradbury", "Robert Heinlein", "Frank Herbert",
        "Harlan Ellison", "Kurt Vonnegut", "Andy Weir", "Liu Cixin",
        "Agatha Christie", "Arthur Conan Doyle", "Marmurek Twain", "Jane Austen",
        "Charles Dickens", "Leo Tolstoy", "Fyodor Dostoyevsky", "George Orwell",
    ]
    titles = [
        "Foundations Edge", "The Expanse", "Ubik", "The Lathe of Heaven",
        "The Hitchhiker's Guide", "Fahrenheit 451", "Stranger in a Strange Land", "Dune",
        "Dangerous Visions", "Slaughterhouse Five", "The Martian", "Three Body Problem",
        "Murder on the Orient Express", "The Hound of Baskervilles", "Adventures of Huckleberry Finn", "Pride and Prejudice",
        "Great Expectations", "War and Peace", "Crime and Punishment", "1984",
        "The Stand", "Neuromancer", "Snow Crash", "The Diamond Age",
        "Leviathan Wakes", "Caliban's War", "Abaddon's Gate", "The Goblin Emperor",
        "The Name of the Wind", "Mistborn", "The Way of Kings", "The Poppy War",
    ]
    publishers = [
        "Penguin Books", "Orbit", "Del Rey", "Tor", "Doubleday",
        "Simon & Schuster", "HarperCollins", "Random House", "Bantam", "Ace",
        "Baen", "Subterranean Press", "TSR", "Wizards of the Coast", "Valve",
    ]

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        author = authors[(row_id - 1) % len(authors)]
        title = titles[(row_id * 5 - 1) % len(titles)]
        publisher = publishers[(row_id * 3 - 1) % len(publishers)]
        # Data publikacji: rozrzucone miedzy 1950 a 2025
        publish_year = 1950 + (row_id % 75)
        publish_month = ((row_id % 12) + 1)
        publish_date = f"{publish_year:04d}-{publish_month:02d}-01"
        # Liczba stron: 200-800
        pages = 200 + (row_id % 600)
        # Czy ksiazka jest w czytelni: 30% szansy, zapis przenosny dla PostgreSQL i MSSQL
        is_in_reading_room = 1 if (row_id % 10) < 3 else 0
        # Dystrybuuj ksiazki po sklepach
        book_shop_id = ((row_id - 1) % len(BOOK_SHOPS)) + 1

        lines.append(
            "INSERT INTO bench.Book "
            "(id, title, author, publisher, publishDate, pages, isInReadingRoom, bookShopId) "
            f"VALUES ({row_id}, '{_sql_quote(title)}', '{_sql_quote(author)}', '{_sql_quote(publisher)}', "
            f"'{publish_date}', {pages}, {is_in_reading_room}, {book_shop_id});"
        )
    return lines


def generate_book_shop_offering_inserts(
    row_count: int,
    book_count: int,
    book_shop_count: int,
) -> list[str]:
    """Generuje inserty dla bench.BookShopOffering.

    Katalog ksiazek w sklepach (many-to-many: ksiazka -> sklepy).
    """
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        # Losuj ksiazke z zakresu wygenerowanych rekordow.
        book_id = ((row_id * 3 - 1) % book_count) + 1
        # Losuj sklep z zakresu wygenerowanych rekordow.
        book_shop_id = ((row_id * 7 - 1) % book_shop_count) + 1

        lines.append(
            "INSERT INTO bench.BookShopOffering (id, bookId, bookShopId) "
            f"SELECT {row_id}, {book_id}, {book_shop_id} "
            "WHERE NOT EXISTS ("
            "SELECT 1 FROM bench.BookShopOffering "
            f"WHERE id = {row_id} OR (bookId = {book_id} AND bookShopId = {book_shop_id})"
            ");"
        )
    return lines


def generate_book_reservation_inserts(
    row_count: int,
    user_count: int,
    book_count: int,
) -> list[str]:
    """Generuje inserty dla bench.BookReservation.

    Rezerwacje ksiazek od uzytkownikow.
    """
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        # Rozklad celowo nierowny: aktywni czytelnicy dostaja wiecej rezerwacji.
        user_id = _pick_user_id_with_activity_skew(
            row_id,
            user_count,
            activity_ratio=0.2,
            step=7,
        )
        book_id = ((row_id * 11 - 1) % book_count) + 1
        # Data rezerwacji: rozrzucone miedzy ostatnie 180 dni
        days_ago = (row_id % 180)
        when_reserved = f"2025-{((12 - (days_ago // 30)) % 12) + 1:02d}-{((days_ago % 28) + 1):02d}"

        lines.append(
            "INSERT INTO bench.BookReservation "
            "(id, whenReserved, userId, bookId) "
            f"VALUES ({row_id}, '{when_reserved}', {user_id}, {book_id});"
        )
    return lines


# Generator insertów do tabeli BookRental
def generate_book_rental_inserts(
    row_count: int,
    user_count: int,
    book_count: int,
    employee_count: int,
    book_shop_count: int,
) -> list[str]:
    """Generuje inserty dla bench.BookRental.
    Każda książka może być wypożyczona przez różnych użytkowników, obsługiwana przez różnych pracowników, w różnych sklepach, różnymi metodami.
    70% wypożyczeń jest już zakończonych (ma endDate), reszta aktywna.
    Komentarze po polsku zgodnie z preferencją użytkownika.
    """
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        # Losowanie powiązań
        user_id = _pick_user_id_with_activity_skew(
            row_id,
            user_count,
            activity_ratio=0.2,
            step=11,
        )
        book_id = ((row_id * 11 - 1) % book_count) + 1
        employee_id = ((row_id * 5 - 1) % employee_count) + 1
        book_shop_id = random.randint(1, book_shop_count)
        rental_method_id = random.randint(1, 2)  # 1 lub 2

        # Daty wypożyczenia (szerszy zakres: 5 lat wstecz)
        days_ago = (row_id % 1825)  # 5 lat * 365 dni
        start_date = f"2025-{((12 - (days_ago // 30)) % 12) + 1:02d}-{((days_ago % 28) + 1):02d}"

        # 70% wypożyczeń już zakończonych
        is_returned = 1 if ((row_id - 1) % 10) < 7 else 0
        if is_returned:
            rental_duration_days = 7 + (row_id % 21)
            end_date = f"2025-{((12 - ((days_ago - rental_duration_days) // 30)) % 12) + 1:02d}-{(((days_ago - rental_duration_days) % 28) + 1):02d}"
            end_date_clause = f"'{end_date}'"
        else:
            end_date_clause = "NULL"

        lines.append(
            "INSERT INTO bench.BookRental "
            "(id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId) "
            f"VALUES ({row_id}, {book_id}, {user_id}, {employee_id}, {book_shop_id}, {is_returned}, '{start_date}', {end_date_clause}, {rental_method_id});"
        )
    return lines





def generate_book_shop_opening_hours_inserts(row_count: int) -> list[str]:
    """Generuje inserty dla bench.BookShopOpeningHours.

    Liczba rekordow powinna byc rowna liczbie sklepow.
    bookShopId jest mapowane 1:1 do id sklepu.
    """
    max_opening_hours = len(BOOK_SHOPS)
    if row_count > max_opening_hours:
        raise ValueError(
            "bench.BookShopOpeningHours obsluguje maksymalnie 10 rekordow w obecnym szkielecie.",
        )

    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        saturday_open = "10:00:00"
        saturday_close = "14:00:00"
        sunday_open = "'11:00:00'" if row_id % 3 == 0 else "NULL"
        sunday_close = "'15:00:00'" if row_id % 3 == 0 else "NULL"

        lines.append(
            "INSERT INTO bench.BookShopOpeningHours "
            "(id, opensAtMonday, closesAtMonday, opensAtTuesday, closesAtTuesday, "
            "opensAtWednesday, closesAtWednesday, opensAtThursday, closesAtThursday, "
            "opensAtFriday, closesAtFriday, opensAtSaturday, closesAtSaturday, "
            "opensAtSunday, closesAtSunday, bookShopId) "
            f"VALUES ({row_id}, '09:00:00', '18:00:00', '09:00:00', '18:00:00', "
            f"'09:00:00', '18:00:00', '09:00:00', '18:00:00', '09:00:00', '18:00:00', "
            f"'{saturday_open}', '{saturday_close}', "
            f"{sunday_open}, {sunday_close}, {row_id});"
        )
    return lines


def generate_book_shop_opening_hours_updates(row_count: int) -> list[str]:
    """Dopina relacje 1:1 miedzy BookShop i BookShopOpeningHours."""
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        lines.append(
            "UPDATE bench.BookShop "
            f"SET openingHoursId = {row_id} WHERE id = {row_id};"
        )
        lines.append(
            "UPDATE bench.BookShopOpeningHours "
            f"SET bookShopId = {row_id} WHERE id = {row_id};"
        )
    return lines


def generate_employee_updates(row_count: int, book_shop_count: int) -> list[str]:
    """Dopina relacje Employee.primaryBookShopId 1:1 do sklepow."""
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        primary_book_shop_id = ((row_id - 1) % book_shop_count) + 1
        lines.append(
            "UPDATE bench.Employee "
            f"SET primaryBookShopId = {primary_book_shop_id} WHERE id = {row_id};"
        )
    return lines


def generate_book_shop_user_updates(row_count: int, book_shop_count: int) -> list[str]:
    """Dopina relacje BookShopUser.mainBookShopId do sklepow."""
    lines: list[str] = []
    for row_id in range(1, row_count + 1):
        main_book_shop_id = ((row_id - 1) % book_shop_count) + 1
        lines.append(
            "UPDATE bench.BookShopUser "
            f"SET mainBookShopId = {main_book_shop_id} WHERE id = {row_id};"
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

    # 5) UserCard
    user_card_count = table_row_counts["UserCard"]
    lines.append(f"-- UserCard: {user_card_count} rows")
    lines.extend(generate_user_card_inserts(user_card_count))
    lines.append("")

    # 6) UserAccount
    user_account_count = table_row_counts["UserAccount"]
    lines.append(f"-- UserAccount: {user_account_count} rows")
    lines.extend(generate_user_account_inserts(user_account_count))
    lines.append("")

    # 7) Employee
    employee_count = table_row_counts["Employee"]
    lines.append(f"-- Employee: {employee_count} rows")
    lines.extend(generate_employee_inserts(employee_count))
    lines.append("")

    # 8) BookShop
    lines.append("-- Stage 3: tables waiting for Employee and BookShopOpeningHours generators")
    book_shop_count = table_row_counts["BookShop"]
    lines.append(f"-- BookShop: {book_shop_count} rows")
    lines.extend(generate_book_shop_inserts(book_shop_count))
    lines.append("")

    # 9) BookShopOpeningHours
    book_shop_opening_hours_count = table_row_counts["BookShopOpeningHours"]
    lines.append(f"-- BookShopOpeningHours: {book_shop_opening_hours_count} rows")
    lines.extend(generate_book_shop_opening_hours_inserts(book_shop_opening_hours_count))
    lines.append("")

    # 10) Synchronizacja BookShop <-> BookShopOpeningHours
    lines.append("-- Stage 4: synchronize BookShop and BookShopOpeningHours")
    lines.extend(generate_book_shop_opening_hours_updates(book_shop_opening_hours_count))
    lines.append("")

    # 11) Book
    book_count = table_row_counts["Book"]
    lines.append(f"-- Book: {book_count} rows")
    lines.extend(generate_book_inserts(book_count))
    lines.append("")

    # 12) BookShopOffering
    book_shop_offering_count = table_row_counts["BookShopOffering"]
    lines.append(f"-- BookShopOffering: {book_shop_offering_count} rows")
    lines.extend(
        generate_book_shop_offering_inserts(
            book_shop_offering_count,
            book_count,
            book_shop_count,
        )
    )
    lines.append("")

    # 13) BookReservation
    book_reservation_count = table_row_counts["BookReservation"]
    lines.append(f"-- BookReservation: {book_reservation_count} rows")
    lines.extend(
        generate_book_reservation_inserts(
            book_reservation_count,
            book_shop_user_count,
            book_count,
        )
    )
    lines.append("")

    # 14) BookRental
    book_rental_count = table_row_counts["BookRental"]
    lines.append(f"-- BookRental: {book_rental_count} rows")
    lines.extend(
        generate_book_rental_inserts(
            book_rental_count,
            book_shop_user_count,
            book_count,
            employee_count,
            book_shop_count,
        )
    )
    lines.append("")

    # 15) Synchronizacja Employee -> BookShop
    lines.append("-- Stage 5: synchronize Employee and BookShop")
    lines.extend(generate_employee_updates(employee_count, book_shop_count))
    lines.append("")

    # 16) Synchronizacja BookShopUser -> BookShop
    lines.append("-- Stage 6: synchronize BookShopUser and BookShop")
    lines.extend(generate_book_shop_user_updates(book_shop_user_count, book_shop_count))
    lines.append("")

    return lines


def generate_relational_sql(
    dataset_size: int,
    table_row_overrides: dict[str, int] | None = None,
    total_rows: int | None = None,
) -> str:
    """Zwraca tresc insertow dla silnikow relacyjnych (PostgreSQL/MSSQL).
    
    Jeśli total_rows jest podane, używa inteligentnego kalkulatora.
    W innym wypadku fallback do starego mechanizmu dataset_size.
    """
    if total_rows is not None:
        # Nowa metoda: inteligentne wyliczanie
        table_row_counts = calculate_table_row_counts(total_rows)
    else:
        # Stara metoda
        table_row_counts = resolve_table_row_counts(dataset_size, table_row_overrides)
    
    return "\n".join(build_shared_insert_lines(table_row_counts)) + "\n"