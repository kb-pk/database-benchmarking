INSERT INTO BookShop (id, shopName, address, email) VALUES
(1, 'Book Haven', '123 Main St', 'bookhaven@email.com'),
(2, 'Readers Paradise', '456 Elm St', 'readersparadise@email.com'),
(3, 'The Book Nook', '789 Oak St','thebooknook@email.com'),
(4, 'Page Turners', '321 Pine St', 'pageturners@email.com'),
(5, 'Literary Lounge', '654 Maple St', 'literarylounge@email.com'),
(6, 'Weekend Reads', '987 Cedar St', 'weekendreads@email.com')

INSERT INTO UserAccountPermissions (id, permission, details) VALUES
(1, 'Rent Books', 'Allows the user to rent books from the shop'),
(2, 'Reserve and Rent Books', 'Allows the user to reserve and rent books in advance'),
(3, 'Blocked', 'User is blocked from renting or reserving books'),
(4, 'Basic Employee', 'Allows the user to manage book inventory and assist customers and rent books to customers'),
(5, 'Administrator', 'Full access to all system features, including user management and reporting'),

INSERT INTO ActivationStatus (id, status) VALUES
(1, 'Active'),
(2, 'Inactive'),
(3, 'Suspended'),
(4, 'Pending Activation'),
(5, 'Deactivated')

INSERT INTO Employee (id, name, surname, phoneNumber, email, birthDate, startedAt, primaryBookShopId, primaryBusinessRole, salary)
SELECT 
    gs.id,
    -- LOSOWANIE IMIENIA I NAZWISKA
    name_val,
    surname_val,
    -- POLSKI NUMER TELEFONU
    (floor(random() * 4) + 5)::text || LPAD(floor(random() * 100)::text, 2, '0') || '-' || 
    LPAD(floor(random() * 1000)::text, 3, '0') || '-' || 
    LPAD(floor(random() * 1000)::text, 3, '0'),
    -- EMAIL: imie.nazwisko.id@poczta.pl
    LOWER(name_val || '.' || surname_val || gs.id || '@poczta.pl'),
    -- DATA URODZENIA (Minimum 18 lat temu, max 65 lat temu)
    birth_date,
    -- DATA ZATRUDNIENIA (Między 18. urodzinami a dziś)
    birth_date + (INTERVAL '18 years') + (random() * (CURRENT_DATE - (birth_date + INTERVAL '18 years'))),
    -- BOOKSHOP ID (1-6)
    (floor(random() * 6) + 1)::int,
    -- ROLE (Lista 10 ról)
    (ARRAY['Manager', 'Księgowa', 'Bibliotekarz', 'Sprzedawca', 'Magazynier', 'Specjalista ds. zamówień', 'Ochroniarz', 'Asystent', 'Kierownik działu', 'Archiwista'])[floor(random() * 10 + 1)],
    -- PŁACA (1500 - 30 000)
    ROUND((random() * (30000 - 1500) + 1500)::numeric, 2)
FROM (
    SELECT 
        id,
        (ARRAY['Piotr', 'Anna', 'Krzysztof', 'Maria', 'Andrzej', 'Katarzyna', 'Tomasz', 'Małgorzata', 'Paweł', 'Agnieszka', 'Jan', 'Barbara', 'Michał', 'Ewa', 'Marcin', 'Magdalena', 'Jakub', 'Elżbieta', 'Adam', 'Joanna', 'Łukasz', 'Zofia', 'Mateusz', 'Natalia', 'Wojciech', 'Monika', 'Robert', 'Justyna', 'Kamil', 'Marta', 'Dariusz', 'Beata', 'Marek', 'Edyta', 'Grzegorz', 'Danuta', 'Sławomir', 'Karolina', 'Jacek', 'Urszula'])[floor(random() * 40 + 1)] as name_val,
        (ARRAY['Nowak', 'Kowalski', 'Wiśniewski', 'Wójcik', 'Kowalczyk', 'Kamiński', 'Lewandowski', 'Zieliński', 'Szymański', 'Woźniak', 'Dąbrowski', 'Kozłowski', 'Mazur', 'Jankowski', 'Kwiatkowski', 'Krawczyk', 'Kaczmarek', 'Piotrowski', 'Grabowski', 'Zając', 'Pawłowski', 'Michalski', 'Król', 'Wieczorek', 'Jabłoński', 'Wróbel', 'Nowakowski', 'Majewski', 'Olszewski', 'Stępień', 'Malinowski', 'Jaworski', 'Adamczyk', 'Dudek', 'Nowicki', 'Pawlak', 'Witkowski', 'Walczak', 'Sikora', 'Baran'])[floor(random() * 40 + 1)] as surname_val,
        -- Generujemy datę urodzenia jako "dziś minus losowa liczba dni" (od 18 do 65 lat temu)
        CURRENT_DATE - (INTERVAL '1 year' * (random() * (65 - 18) + 18)) as birth_date
    FROM generate_series(1, 190) AS id
) AS gs;