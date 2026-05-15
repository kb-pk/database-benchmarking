package bench.app.service.userpermission;

import java.util.Map;

public interface UserPermissionCrudOperations {
    Map<String, Object> create(Integer requestedId, String permission, String details);

    Map<String, Object> createRentalMethod(Integer requestedId, String method);

    Map<String, Object> createBookShop(Integer requestedId, String shopName, String address, String email, Integer managerId);

    Map<String, Object> createUserRegistration(
            Integer requestedId,
            String name,
            String surname,
            String phoneNumber,
            String email,
            String login,
            String passwordHash,
            String cardIdNumber,
            Integer activationStatusId,
            Integer permissionId,
            Integer mainBookShopId
    );

    // CREATE 5 - Dodanie książek do oferty sklepu
    Map<String, Object> createBookWithOffering(
            Integer requestedBookId,
            String title,
            String author,
            Integer requestedOfferingId,
            Integer bookShopId
    );

    // CREATE 6 - Dodanie rezerwacji książki przez użytkownika
    Map<String, Object> createBookReservation(
            Integer requestedReservationId,
            Integer bookId,
            Integer userId
    );

    // CREATE 7 - Utworzenie wypożyczenia z pełnym kontekstem
    Map<String, Object> createRentalWithFullContext(
            Integer requestedRentalId,
            Integer bookId,
            Integer userId,
            Integer employeeId,
            Integer shopId,
            Integer rentalMethodId
    );

    // CREATE 8 - Warunkowe utworzenie wypożyczenia
    Map<String, Object> createConditionalRental(
            Integer requestedRentalId
    );

    // CREATE 9 - Batch dostawy do sklepu
    Map<String, Object> createBatchSupplyEvent(
            Integer startBookId,
            Integer numberOfBooks,
            Integer shopId
    );

    Map<String, Object> read(int id);

    Map<String, Object> update(int id, String permission, String details);

    Map<String, Object> delete(int id);
}
