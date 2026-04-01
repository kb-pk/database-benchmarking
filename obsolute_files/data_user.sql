INSERT INTO User (id, name, surname, phoneNumber, email, mainBookShopId, isActiveId)
SELECT 
    gs.id, 
    (ARRAY[
        'Piotr', 'Anna', 'Krzysztof', 'Maria', 'Andrzej', 'Katarzyna', 'Tomasz', 'Małgorzata', 
        'Paweł', 'Agnieszka', 'Jan', 'Barbara', 'Michał', 'Ewa', 'Marcin', 'Magdalena', 
        'Jakub', 'Elżbieta', 'Adam', 'Joanna'
    ])[floor(random() * 20 + 1)], -- Losowe imię z listy 20 popularnych imion w Polsce

    (ARRAY[
        'Nowak', 'Kowalski', 'Wiśniewski', 'Wójcik', 'Kowalczyk', 'Kamiński', 'Lewandowski', 'Zieliński', 
        'Szymański', 'Woźniak', 'Dąbrowski', 'Kozłowski', 'Mazur', 'Jankowski', 'Kwiatkowski', 'Krawczyk', 
        'Kaczmarek', 'Piotrowski', 'Grabowski', 'Zając', 'Pawłowski', 'Michalski', 'Król', 'Wieczorek', 
        'Jabłoński', 'Wróbel', 'Nowakowski', 'Majewski', 'Olszewski', 'Stępień', 'Malinowski', 'Jaworski', 
        'Adamczyk', 'Dudek', 'Nowicki', 'Pawlak', 'Witkowski', 'Walczak', 'Sikora', 'Baran'
    ])[floor(random() * 40 + 1)], -- Losowe nazwisko z listy 40 popularnych nazwisk w Polsce

    LPAD(floor(random() * 100)::text, 2, '0') || '-' || 
    LPAD(floor(random() * 1000)::text, 3, '0') || '-' || 
    LPAD(floor(random() * 1000)::text, 3, '0'), -- Przykładowy numer telefonu

    LOWER(name_val || '.' || surname_val || gs.id || '@poczta.pl'), -- Przykładowy email:
    (floor(random() * 6) + 1)::int,                 -- mainBookShopId: zakres 1-6
    (floor(random() * 5) + 1)::int,                 -- isActiveId: zakres 1-5
FROM generate_series(1, 800) AS gs(id);


INSERT INTO UserCard (id, cardIdNumber, userId, isActiveId)
SELECT 
    gs.id, 
    -- Unikatowy numer karty (np. CARD-2024-XXXXX)
    'CARD-' || TO_CHAR(CURRENT_DATE, 'YYYY') || '-' || LPAD(gs.id::text, 6, '0'),
    
    -- userId rośnie do 800, potem są NULLe
    CASE 
        WHEN gs.id <= 800 THEN gs.id 
        ELSE NULL 
    END,
    
    -- isActiveId w zakresie 1-5
    (floor(random() * 5) + 1)::int
FROM generate_series(1, 1000) AS gs(id);



INSERT INTO UserAccount (id, login, passwordHash, userId, permissionsId)
SELECT 
    gs.id, 
    'user' || gs.id, -- Przykładowy login: user1, user2, ...
    -- Przykładowy hash hasła (w rzeczywistości powinien być bezpieczny)
    'hash' || LPAD(gs.id::text, 6, '0'), 
    -- userId rośnie do 800, potem są NULLe
    CASE 
        WHEN gs.id <= 800 THEN gs.id 
        ELSE ((gs.id - 1) % 800) + 1 
    END,
    -- permissionsId w zakresie 1-5
    (floor(random() * 5) + 1)::int
FROM generate_series(1, 800) AS gs(id);


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