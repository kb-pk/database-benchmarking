CREATE TYPE day_name AS ENUM (
       'Monday',
       'Tuesday',
       'Wednesday',
       'Thursday',
       'Friday',
       'Saturday',
       'Sunday'
)

CREATE TABLE Book (
    id INT PRIMARY KEY NOT NULL,
    author VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    publisher VARCHAR(255),
    publishDate DATE,
    pages INT,
    isInReadingRoom BIT NOT NULL,
    bookShopId INT NOT NULL REFERENCES BookShop(id)
)

CREATE TABLE BookShop (
    id INT PRIMARY KEY NOT NULL,
    shopName VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    managerId INT REFERENCES Employee(id)
)

CREATE TABLE BookShopOffering (
    id INT PRIMARY KEY NOT NULL,
    bookId INT NOT NULL REFERENCES Book(id),
    bookShopId INT NOT NULL REFERENCES BookShop(id)
)

CREATE TABLE BookReservations (
    id INT NOT NULL PRIMARY KEY,
    bookId INT NOT NULL REFERENCES Book(id),
    userId INT NOT NULL REFERENCES User(id),
    whenReserved DATE NOT NULL
)

CREATE TABLE BookRentals (
    id INT NOT NULL PRIMARY KEY,
    bookId INT NOT NULL REFERENCES Book(id),
    userId INT NOT NULL REFERENCES User(id),
    employee INT NOT NULL REFERENCES Employee(id),
    bookShop INT NOT NULL REFERENCES BookShop(id),
    isReturned BIT NOT NULL,
    startDate DATE NOT NULL,
    endDate DATE NOT NULL,
    rentalMethodId INT NOT NULL REFERENCES BookRentalMethod(id)
)

CREATE TABLE BookRentalMethod (
    id NOT NULL PRIMARY KEY,
    method VARCHAR(255) NOT NULL
)

CREATE TABLE Employee (
    id INT PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    phoneNumber VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    birthDate DATE,
    startedAt DATE,
    primaryBookShopId INT REFERENCES BookShop(id),
    primaryBusinessRole VARCHAR(255),
    salary DECIMAL
)

CREATE TABLE User (
    id INT PRIMARY KEY NOT NULL,
    name VARCHAR(255) NOT NULL,
    surname VARCHAR(255) NOT NULL,
    phoneNumber VARCHAR(255),
    email VARCHAR(255) NOT NULL,
    mainBookShopId INT REFERENCES BookShop(id),
    isActiveId INT NOT NULL REFERENCES ActivationStatus(id),
)

CREATE TABLE UserCard (
    id INT NOT NULL PRIMARY KEY,
    cardIdNumber CHAR(30) NOT NULL,
    userId INT REFERENCES User(id), -- moga buc karty bez uzytkownika
    isActiveId INT NOT NULL REFERENCES ActivationStatus(id)
)

CREATE TABLE UserAccount (
    id INT NOT NULL PRIMARY KEY,
    login VARCHAR(255) NOT NULL,
    passwordHash CHAR(255) NOT NULL,
    userId INT NOT NULL REFERENCES User(id),
    permissionsId INT NOT NULL REFERENCES UserAccountPermissions(id)
)

CREATE TABLE UserAccountPermissions (
    id INT NOT NULL PRIMARY KEY,
    permission VARCHAR(255) NOT NULL,
    details VARCHAR(255)
)

CREATE TABLE ActivationStatus (
    id INT NOT NULL PRIMARY KEY,
    status VARCHAR(255)
)