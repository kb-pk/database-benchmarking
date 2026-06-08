CREATE KEYSPACE IF NOT EXISTS bench
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};

USE bench;

CREATE TABLE IF NOT EXISTS bookshops (
    shop_id uuid PRIMARY KEY,
    shop_name text,
    address text,
    email text,
    manager_id uuid,
    opens_at_monday time,
    closes_at_monday time,
    opens_at_tuesday time,
    closes_at_tuesday time,
    opens_at_wednesday time,
    closes_at_wednesday time,
    opens_at_thursday time,
    closes_at_thursday time,
    opens_at_friday time,
    closes_at_friday time,
    opens_at_saturday time,
    closes_at_saturday time,
    opens_at_sunday time,
    closes_at_sunday time
);

CREATE TABLE IF NOT EXISTS books_by_shop (
    shop_id uuid,
    book_id uuid,
    author text,
    title text,
    publisher text,
    publish_date date,
    pages int,
    is_in_reading_room boolean,
    PRIMARY KEY ((shop_id), book_id)
);

CREATE TABLE IF NOT EXISTS users (
    user_id uuid PRIMARY KEY,
    name text,
    surname text,
    phone_number text,
    email text,
    main_book_shop_id uuid,
    card_id_number text,
    status text,
    login text,
    permissions set<text>
);

CREATE TABLE IF NOT EXISTS user_credentials_by_login (
    login text PRIMARY KEY,
    user_id uuid,
    password_hash text,
    status text
);

CREATE TABLE IF NOT EXISTS employees_by_shop (
    primary_book_shop_id uuid,
    employee_id uuid,
    name text,
    surname text,
    phone_number text,
    email text,
    birth_date date,
    started_at date,
    primary_business_role text,
    PRIMARY KEY ((primary_book_shop_id), employee_id)
);

CREATE TABLE IF NOT EXISTS rentals_by_user (
     user_id uuid,
     start_date date,
     rental_id uuid,
     book_id uuid,
     book_title text,
     shop_id uuid,
     employee_id uuid,
     is_returned boolean,
     end_date date,
     rental_method text,
     PRIMARY KEY ((user_id), start_date, rental_id)
) WITH CLUSTERING ORDER BY (start_date DESC);

CREATE TABLE IF NOT EXISTS rentals_by_shop (
    shop_id uuid,
    start_date date,
    rental_id uuid,
    book_id uuid,
    user_id uuid,
    employee_id uuid,
    is_returned boolean,
    end_date date,
    rental_method text,
    PRIMARY KEY ((shop_id), start_date, rental_id)
) WITH CLUSTERING ORDER BY (start_date DESC);

CREATE TABLE IF NOT EXISTS reservations_by_user (
    user_id uuid,
    when_reserved date,
    reservation_id uuid,
    book_id uuid,
    book_title text,
    PRIMARY KEY ((user_id), when_reserved, reservation_id)
) WITH CLUSTERING ORDER BY (when_reserved DESC);

-- Indeksy wtórne pod operacje benchmarkowe (Cassandra/Scylla)

-- R2, U6: filtrowanie użytkowników po sklepie głównym
CREATE INDEX IF NOT EXISTS idx_users_main_book_shop_id ON users (main_book_shop_id);

-- U3, D4: szybkie wyszukiwanie po statusie użytkownika
CREATE INDEX IF NOT EXISTS idx_users_status ON users (status);

-- R6: filtrowanie aktywności po przedziale dat
CREATE INDEX IF NOT EXISTS idx_reservations_by_user_when_reserved ON reservations_by_user (when_reserved);
CREATE INDEX IF NOT EXISTS idx_rentals_by_user_start_date ON rentals_by_user (start_date);

-- R5/C4: lookup książki po book_id bez znajomości shop_id
CREATE INDEX IF NOT EXISTS idx_books_by_shop_book_id ON books_by_shop (book_id);

-- D6: kasowanie/wyszukiwanie wypożyczeń po employee_id i dniu
CREATE INDEX IF NOT EXISTS idx_rentals_by_shop_employee_id ON rentals_by_shop (employee_id);

-- U3, U5: szybkie zawężenie do niezwróconych wypożyczeń
CREATE INDEX IF NOT EXISTS idx_rentals_by_user_is_returned ON rentals_by_user (is_returned);

-- D1/D2: usuwanie rekordów po ID bez klucza partycji
CREATE INDEX IF NOT EXISTS idx_reservations_by_user_reservation_id ON reservations_by_user (reservation_id);
CREATE INDEX IF NOT EXISTS idx_rentals_by_user_rental_id ON rentals_by_user (rental_id);

-- U4: wyszukiwanie pracownika bez znajomości primary_book_shop_id
CREATE INDEX IF NOT EXISTS idx_employees_by_shop_employee_id ON employees_by_shop (employee_id);
