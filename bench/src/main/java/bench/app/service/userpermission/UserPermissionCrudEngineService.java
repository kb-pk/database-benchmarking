package bench.app.service.userpermission;

import java.util.Map;

public interface UserPermissionCrudEngineService {
    boolean supports(DatabaseEngine engine);

    Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details);

    Map<String, Object> createRentalMethod(DatabaseEngine engine, Integer requestedId, String method);

    Map<String, Object> createBookShop(
            DatabaseEngine engine,
            Integer requestedId,
            String shopName,
            String address,
            String email,
            Integer managerId
    );

        Map<String, Object> createUserRegistration(
            DatabaseEngine engine,
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
            DatabaseEngine engine,
            Integer requestedBookId,
            String title,
            String author,
            Integer requestedOfferingId,
            Integer bookShopId
    );

    // CREATE 6 - Dodanie rezerwacji książki przez użytkownika
    Map<String, Object> createBookReservation(
            DatabaseEngine engine,
            Integer requestedReservationId,
            Integer bookId,
            Integer userId
    );

    // CREATE 7 - Utworzenie wypożyczenia z pełnym kontekstem
    Map<String, Object> createRentalWithFullContext(
            DatabaseEngine engine,
            Integer requestedRentalId,
            Integer bookId,
            Integer userId,
            Integer employeeId,
            Integer shopId,
            Integer rentalMethodId
    );

    // CREATE 8 - Warunkowe utworzenie wypożyczenia
    Map<String, Object> createConditionalRental(
            DatabaseEngine engine,
            Integer requestedRentalId
    );

    // CREATE 9 - Batch dostawy do sklepu
    Map<String, Object> createBatchSupplyEvent(
            DatabaseEngine engine,
            Integer startBookId,
            Integer numberOfBooks,
            Integer shopId
    );

    Map<String, Object> read(DatabaseEngine engine, int id);

    Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details);

    Map<String, Object> delete(DatabaseEngine engine, int id);
}
