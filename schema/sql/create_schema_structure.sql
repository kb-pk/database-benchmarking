CREATE TABLE bench.BookShopOpeningHours (
    id INT NOT NULL,
    opensAtMonday TIME,
    closesAtMonday TIME,
    opensAtTuesday TIME,
    closesAtTuesday TIME,
    opensAtWednesday TIME,
    closesAtWednesday TIME,
    opensAtThursday TIME,
    closesAtThursday TIME,
    opensAtFriday TIME,
    closesAtFriday TIME,
    opensAtSaturday TIME,
    closesAtSaturday TIME,
    opensAtSunday TIME,
    closesAtSunday TIME,
    bookShopId INT NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.Book (
    id INT NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    publishDate DATE,
    pages INT,
    isInReadingRoom SMALLINT NOT NULL,
    bookShopId INT NOT NULL,

    PRIMARY KEY (id),
    CHECK (isInReadingRoom IN (0, 1))
);

CREATE TABLE bench.BookShop (
    id INT NOT NULL,
    shopName VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    managerId INT NOT NULL,
    openingHoursId INT,

    PRIMARY KEY (id)
);

CREATE TABLE bench.BookShopOffering (
    id INT NOT NULL,
    bookId INT NOT NULL,
    bookShopId INT NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.BookReservation(
    id INT NOT NULL,
    bookId INT NOT NULL,
    userId INT NOT NULL,
    whenReserved DATE NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.BookRental(
    id INT NOT NULL,
    bookId INT NOT NULL,
    userId INT NOT NULL,
    employeeId INT NOT NULL,
    bookShopId INT NOT NULL,
    isReturned SMALLINT NOT NULL,
    startDate DATE NOT NULL,
    endDate DATE,
    rentalMethodId INT NOT NULL,

    PRIMARY KEY (id),
    CHECK (isReturned IN (0, 1))
);

CREATE TABLE bench.BookRentalMethod (
    id INT NOT NULL,
    method VARCHAR(255) NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.Employee (
    id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    phoneNumber VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birthDate DATE,
    startedAt DATE,
    primaryBookShopId INT,
    primaryBusinessRole VARCHAR(255),

    PRIMARY KEY (id)
);

CREATE TABLE bench.BookShopUser (
    id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    phoneNumber VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    mainBookShopId INT,
    isActiveId INT NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.UserCard (
    id INT NOT NULL,
    cardIdNumber CHAR(30) NOT NULL,
    userId INT,
    isActiveId INT NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.UserAccount (
    id INT NOT NULL,
    login VARCHAR(255) NOT NULL,
    passwordHash CHAR(255) NOT NULL,
    userId INT NOT NULL,
    permissionsId INT NOT NULL,

    PRIMARY KEY (id)
);

CREATE TABLE bench.UserAccountPermissions (
    id INT NOT NULL,
    permission VARCHAR(255) NOT NULL,
    details VARCHAR(255),

    PRIMARY KEY (id)
);

CREATE TABLE bench.ActivationStatus (
    id INT NOT NULL,
    status VARCHAR(255),

    PRIMARY KEY (id)
);

ALTER TABLE bench.BookShopOpeningHours
    ADD CONSTRAINT bookshopopeninghours_fk1 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id);

ALTER TABLE bench.Book
    ADD CONSTRAINT book_fk1 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id);

ALTER TABLE bench.BookShop
    ADD CONSTRAINT bookshop_fk1 FOREIGN KEY (managerId) REFERENCES bench.Employee(id);
ALTER TABLE bench.BookShop
    ADD CONSTRAINT bookshop_fk2 FOREIGN KEY (openingHoursId) REFERENCES bench.BookShopOpeningHours(id);

ALTER TABLE bench.BookShopOffering
    ADD CONSTRAINT bookshopoffering_fk1 FOREIGN KEY (bookId) REFERENCES bench.Book(id);
ALTER TABLE bench.BookShopOffering
    ADD CONSTRAINT bookshopoffering_fk2 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id);

ALTER TABLE bench.BookReservation
    ADD CONSTRAINT bookreservation_fk1 FOREIGN KEY (bookId) REFERENCES bench.Book(id);
ALTER TABLE bench.BookReservation
    ADD CONSTRAINT bookreservation_fk2 FOREIGN KEY (userId) REFERENCES bench.BookShopUser(id);

ALTER TABLE bench.BookRental
    ADD CONSTRAINT bookrental_fk1 FOREIGN KEY (bookId) REFERENCES bench.Book(id);
ALTER TABLE bench.BookRental
    ADD CONSTRAINT bookrental_fk2 FOREIGN KEY (userId) REFERENCES bench.BookShopUser(id);
ALTER TABLE bench.BookRental
    ADD CONSTRAINT bookrental_fk3 FOREIGN KEY (employeeId) REFERENCES bench.Employee(id);
ALTER TABLE bench.BookRental
    ADD CONSTRAINT bookrental_fk4 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id);
ALTER TABLE bench.BookRental
    ADD CONSTRAINT bookrental_fk5 FOREIGN KEY (rentalMethodId) REFERENCES bench.BookRentalMethod(id);

ALTER TABLE bench.Employee
    ADD CONSTRAINT employee_fk1 FOREIGN KEY (primaryBookShopId) REFERENCES bench.BookShop(id);

ALTER TABLE bench.BookShopUser
    ADD CONSTRAINT bookshopuser_fk1 FOREIGN KEY (mainBookShopId) REFERENCES bench.BookShop(id);
ALTER TABLE bench.BookShopUser
    ADD CONSTRAINT bookshopuser_fk2 FOREIGN KEY (isActiveId) REFERENCES bench.ActivationStatus(id);

ALTER TABLE bench.UserCard
    ADD CONSTRAINT usercard_fk1 FOREIGN KEY (userId) REFERENCES bench.BookShopUser(id);
ALTER TABLE bench.UserCard
    ADD CONSTRAINT usercard_fk2 FOREIGN KEY (isActiveId) REFERENCES bench.ActivationStatus(id);

ALTER TABLE bench.UserAccount
    ADD CONSTRAINT useraccount_fk1 FOREIGN KEY (userId) REFERENCES bench.BookShopUser(id);
ALTER TABLE bench.UserAccount
    ADD CONSTRAINT useraccount_fk2 FOREIGN KEY (permissionsId) REFERENCES bench.UserAccountPermissions(id);

-- Indeksy wydajnosciowe pod benchmark CRUD (PostgreSQL i MSSQL)

-- R1, R5, C6: filtrowanie i joiny po sklepie/ksiazce
CREATE INDEX idx_book_bookshopid_id ON bench.Book (bookShopId, id);
CREATE INDEX idx_bookrental_bookid ON bench.BookRental (bookId);

-- R2, U6, D4: segmentacja uzytkownikow po sklepie i statusie
CREATE INDEX idx_bookshopuser_mainshop_active_id ON bench.BookShopUser (mainBookShopId, isActiveId, id);
CREATE INDEX idx_bookshopuser_active_id ON bench.BookShopUser (isActiveId, id);

-- R3, R6, U3, D3: aktywnosc usera w rezerwacjach/wypozyczeniach + zakresy dat
CREATE INDEX idx_bookreservation_user_whenreserved ON bench.BookReservation (userId, whenReserved);
CREATE INDEX idx_bookrental_user_startdate ON bench.BookRental (userId, startDate);
CREATE INDEX idx_bookrental_bookshopid_startdate ON bench.BookRental (bookShopId, startDate, id);

-- D3: kasowanie starych rezerwacji bez finalizacji (NOT EXISTS na BookRental)
CREATE INDEX idx_bookreservation_whenreserved_bookid_userid ON bench.BookReservation (whenReserved, bookId, userId);
CREATE INDEX idx_bookrental_bookid_userid_startdate ON bench.BookRental (bookId, userId, startDate);

-- R4, D6: obciazenie pracownikow i kasowanie po dniu
CREATE INDEX idx_bookrental_employee_startdate ON bench.BookRental (employeeId, startDate);

-- U5: szybkie wyszukiwanie otwartych, przeterminowanych wypozyczen
CREATE INDEX idx_bookrental_isreturned_startdate ON bench.BookRental (isReturned, startDate);

-- U1
CREATE INDEX idx_useraccount_userid ON bench.UserAccount (userId);

-- U2: laczenie sklepu z godzinami otwarcia
CREATE INDEX idx_bookshop_openinghoursid ON bench.BookShop (openingHoursId);

-- U4
CREATE INDEX idx_employee_primarybookshopid_id ON bench.Employee (primaryBookShopId, id);

-- D5: usuwanie oferty po wzorcu wypozyczen usera
CREATE INDEX idx_bookshopoffering_bookid_bookshopid ON bench.BookShopOffering (bookId, bookShopId);
