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
    salary DECIMAL NOT NULL,

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
    ADD CONSTRAINT bookshopopeninghours_fk1 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id) DEFERRABLE INITIALLY DEFERRED;

ALTER TABLE bench.Book
    ADD CONSTRAINT book_fk1 FOREIGN KEY (bookShopId) REFERENCES bench.BookShop(id);

ALTER TABLE bench.BookShop
    ADD CONSTRAINT bookshop_fk1 FOREIGN KEY (managerId) REFERENCES bench.Employee(id);
ALTER TABLE bench.BookShop
    ADD CONSTRAINT bookshop_fk2 FOREIGN KEY (openingHoursId) REFERENCES bench.BookShopOpeningHours(id) DEFERRABLE INITIALLY DEFERRED;

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
