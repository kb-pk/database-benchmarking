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
