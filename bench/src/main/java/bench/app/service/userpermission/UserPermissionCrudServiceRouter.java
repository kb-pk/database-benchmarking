package bench.app.service.userpermission;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserPermissionCrudServiceRouter implements UserPermissionCrudOperations {
    private final DatabaseEngine activeEngine;
    private final UserPermissionCrudEngineService delegate;

    public UserPermissionCrudServiceRouter(
            BenchmarkEngineResolver engineResolver,
            List<UserPermissionCrudEngineService> services
    ) {
        this.activeEngine = engineResolver.resolveEngine();
        this.delegate = services.stream()
                .filter(service -> service.supports(activeEngine))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Brak serwisu CRUD dla silnika " + activeEngine.propertyValue()
                ));
    }

    @Override
    public Map<String, Object> create(Integer requestedId, String permission, String details) {
        return delegate.create(activeEngine, requestedId, permission, details);
    }

    @Override
    public Map<String, Object> createRentalMethod(Integer requestedId, String method) {
        return delegate.createRentalMethod(activeEngine, requestedId, method);
    }

    @Override
    public Map<String, Object> createBookShop(Integer requestedId, String shopName, String address, String email, Integer managerId) {
        return delegate.createBookShop(activeEngine, requestedId, shopName, address, email, managerId);
    }

    @Override
    public Map<String, Object> createUserRegistration(
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
        return delegate.createUserRegistration(
                activeEngine,
                requestedId,
                name,
                surname,
                phoneNumber,
                email,
                login,
                passwordHash,
                cardIdNumber,
                activationStatusId,
                permissionId,
                mainBookShopId
        );
    }

    @Override
    public Map<String, Object> createBookWithOffering(
            Integer requestedBookId,
            String title,
            String author,
            Integer requestedOfferingId,
            Integer bookShopId
    ) {
        return delegate.createBookWithOffering(
                activeEngine,
                requestedBookId,
                title,
                author,
                requestedOfferingId,
                bookShopId
        );
    }

    @Override
    public Map<String, Object> createBookReservation(
            Integer requestedReservationId,
            Integer bookId,
            Integer userId
    ) {
        return delegate.createBookReservation(
                activeEngine,
                requestedReservationId,
                bookId,
                userId
        );
    }

    @Override
    public Map<String, Object> createRentalWithFullContext(
            Integer requestedRentalId,
            Integer bookId,
            Integer userId,
            Integer employeeId,
            Integer shopId,
            Integer rentalMethodId
    ) {
        return delegate.createRentalWithFullContext(
                activeEngine,
                requestedRentalId,
                bookId,
                userId,
                employeeId,
                shopId,
                rentalMethodId
        );
    }

    @Override
    public Map<String, Object> createConditionalRental(
            Integer requestedRentalId
    ) {
        return delegate.createConditionalRental(
                activeEngine,
                requestedRentalId
        );
    }

    @Override
    public Map<String, Object> createBatchSupplyEvent(
            Integer startBookId,
            Integer numberOfBooks,
            Integer shopId
    ) {
        return delegate.createBatchSupplyEvent(
                activeEngine,
                startBookId,
                numberOfBooks,
                shopId
        );
    }

    @Override
    public Map<String, Object> read(int id) {
        return delegate.read(activeEngine, id);
    }

    @Override
    public Map<String, Object> update(int id, String permission, String details) {
        return delegate.update(activeEngine, id, permission, details);
    }

    @Override
    public Map<String, Object> delete(int id) {
        return delegate.delete(activeEngine, id);
    }
}
