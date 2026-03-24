INSERT INTO Book (id, author, title, publisher, publishDate, pages, isInReadingRoom, bookShopId)
SELECT 
    gs.id,
    -- LOSOWY AUTOR (Kombinacja Imię + Nazwisko)
    (ARRAY['Jan', 'Maria', 'Andrzej', 'Anna', 'Stanisław', 'Katarzyna', 'Jerzy', 'Małgorzata', 'Stefan', 'Barbara'])[floor(random() * 10 + 1)] || ' ' ||
    (ARRAY['Nowak', 'Kowalski', 'Wiśniewski', 'Wójcik', 'Kowalczyk', 'Kamiński', 'Lewandowski', 'Zieliński', 'Szymański', 'Woźniak'])[floor(random() * 10 + 1)],
    
    -- LOSOWY TYTUŁ (Przymiotnik + Rzeczownik)
    (ARRAY['Tajemniczy', 'Ostatni', 'Złoty', 'Cichy', 'Wielki', 'Zapomniany', 'Błękitny', 'Krwawy', 'Mroczny', 'Niezwykły'])[floor(random() * 10 + 1)] || ' ' ||
    (ARRAY['Ogród', 'Zamek', 'Świt', 'Cień', 'Miecz', 'Labirynt', 'Pamiętnik', 'Las', 'Wiatr', 'Płomień'])[floor(random() * 10 + 1)],
    
    -- WYDAWNICTWO
    (ARRAY['PWN', 'Znak', 'Muza', 'Wydawnictwo Literackie', 'Rebis', 'Helion', 'Czarna Owca'])[floor(random() * 7 + 1)],
    
    -- DATA PUBLIKACJI (Ostatnie 40 lat)
    CURRENT_DATE - (random() * INTERVAL '40 years'),
    
    -- LICZBA STRON (od 50 do 1200)
    (floor(random() * 1150) + 50)::int,
    
    -- CZY W CZYTELNI (PostgreSQL BIT(1) wymaga '0' lub '1') i mniej więcej 20% książek jest w czytelni
    (CASE WHEN random() > 0.8 THEN B'1' ELSE B'0' END),
    
    -- BOOKSHOP ID (1-6)
    (floor(random() * 6) + 1)::int

FROM generate_series(1, 100000) AS gs(id);