CREATE KEYSPACE IF NOT EXISTS bench
    WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 3};

USE bench;

CREATE TABLE bookshops (
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

CREATE TABLE books_by_shop (
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

CREATE TABLE users (
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

CREATE TABLE user_credentials_by_login (
    login text PRIMARY KEY,
    user_id uuid,
    password_hash text,
    status text
);

CREATE TABLE employees_by_shop (
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

CREATE TABLE rentals_by_user (
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

CREATE TABLE rentals_by_shop (
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

CREATE TABLE reservations_by_user (
    user_id uuid,
    when_reserved date,
    reservation_id uuid,
    book_id uuid,
    book_title text,
    PRIMARY KEY ((user_id), when_reserved, reservation_id)
) WITH CLUSTERING ORDER BY (when_reserved DESC);
