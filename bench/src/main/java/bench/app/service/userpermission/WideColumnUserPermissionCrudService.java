package bench.app.service.userpermission;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class WideColumnUserPermissionCrudService implements UserPermissionCrudEngineService {
    @Override
    public boolean supports(DatabaseEngine engine) {
        return engine == DatabaseEngine.CASSANDRA || engine == DatabaseEngine.SCYLLA;
    }

    @Override
    public Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createRentalMethod(DatabaseEngine engine, Integer requestedId, String method) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createBookShop(
            DatabaseEngine engine,
            Integer requestedId,
            String shopName,
            String address,
            String email,
            Integer managerId
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createUserRegistration(
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
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> read(DatabaseEngine engine, int id) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> delete(DatabaseEngine engine, int id) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createBookWithOffering(
            DatabaseEngine engine,
            Integer requestedBookId,
            String title,
            String author,
            Integer requestedOfferingId,
            Integer bookShopId
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createBookReservation(
            DatabaseEngine engine,
            Integer requestedReservationId,
            Integer bookId,
            Integer userId
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createRentalWithFullContext(
            DatabaseEngine engine,
            Integer requestedRentalId,
            Integer bookId,
            Integer userId,
            Integer employeeId,
            Integer shopId,
            Integer rentalMethodId
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createConditionalRental(
            DatabaseEngine engine,
            Integer requestedRentalId
    ) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> createBatchSupplyEvent(
            DatabaseEngine engine,
            Integer startBookId,
            Integer numberOfBooks,
            Integer shopId
    ) {
        throw unsupported(engine);
    }

    private ResponseStatusException unsupported(DatabaseEngine engine) {
        return new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "CRUD useraccountpermissions nie jest jeszcze zaimplementowany dla silnika " + engine.propertyValue()
        );
    }
}
