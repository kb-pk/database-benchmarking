INSERT INTO BookShop (id, shopName, address, email, managerId) VALUES
(1, 'Book Haven', '123 Main St', 'bookhaven@email.com', 1),
(2, 'Readers Paradise', '456 Elm St', 'readersparadise@email.com', 2),
(3, 'The Book Nook', '789 Oak St','thebooknook@email.com', 3),
(4, 'Page Turners', '321 Pine St', 'pageturners@email.com', 4),
(5, 'Literary Lounge', '654 Maple St', 'literarylounge@email.com', 5),
(6, 'Weekend Reads', '987 Cedar St', 'weekendreads@email.com', 6)

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