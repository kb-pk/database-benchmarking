package bench.app.service.userpermission;

import bench.app.util.CsvCrudLogger;
import jakarta.annotation.PreDestroy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Time;
import java.sql.Types;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JdbcUserPermissionCrudService implements UserPermissionCrudEngineService {
    private static final int CREATE_REPETITIONS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final UserPermissionQueryCatalog queryCatalog;
    private final BenchmarkEngineResolver engineResolver;
    private final AtomicInteger createOperationCounter = new AtomicInteger(1);
    private final AtomicInteger readOperationCounter = new AtomicInteger(1);
    private final AtomicInteger updateOperationCounter = new AtomicInteger(1);
    private final AtomicInteger deleteOperationCounter = new AtomicInteger(1);

    private CsvCrudLogger csvLogger;

    public JdbcUserPermissionCrudService(
            JdbcTemplate jdbcTemplate,
            UserPermissionQueryCatalog queryCatalog,
            BenchmarkEngineResolver engineResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryCatalog = queryCatalog;
        this.engineResolver = engineResolver;
    }

    @Override
    public boolean supports(DatabaseEngine engine) {
        return engine == DatabaseEngine.POSTGRESQL || engine == DatabaseEngine.MSSQL;
    }

    @Override
    public Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details) {
        validatePermission(permission);

        // SELECT_MAX_ID: pobranie najwyzszego id, zeby wyliczyc pierwsze id do INSERT.
        Integer maxId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_ID),
                Integer.class
        );
        int nextId = (maxId == null ? 0 : maxId) + 1;
        int firstIdToInsert = (requestedId == null || requestedId <= 0) ? nextId : requestedId;
        List<Integer> insertedIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentId = firstIdToInsert + i;
                Instant start = Instant.now();
                int affectedRows = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE),
                        currentId,
                        permission,
                        details
                );
                Instant end = Instant.now();

                if (affectedRows != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedIds.add(currentId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "ids", insertedIds,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs),
                    "permission", permission
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create permission: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> createRentalMethod(DatabaseEngine engine, Integer requestedId, String method) {
        validateRentalMethod(method);

        // SELECT_MAX_RENTAL_METHOD_ID: analogicznie wyliczamy kolejne id dla bench.bookrentalmethod.
        Integer maxId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_RENTAL_METHOD_ID),
                Integer.class
        );
        int nextId = (maxId == null ? 0 : maxId) + 1;
        int firstIdToInsert = (requestedId == null || requestedId <= 0) ? nextId : requestedId;
        List<Integer> insertedIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentId = firstIdToInsert + i;
                String currentMethod = method + "_" + currentId;
                Instant start = Instant.now();
                int affectedRows = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_RENTAL_METHOD),
                        currentId,
                        currentMethod
                );
                Instant end = Instant.now();

                if (affectedRows != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert rental method failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedIds.add(currentId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 2,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "ids", insertedIds,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs),
                    "method_base", method
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create rental method: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Map<String, Object> createBookShop(
            DatabaseEngine engine,
            Integer requestedId,
            String shopName,
            String address,
            String email,
            Integer managerId
    ) {
        validateBookShop(shopName, address, email);

        Integer maxBookShopId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOKSHOP_ID),
                Integer.class
        );
        Integer maxOpeningHoursId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_OPENING_HOURS_ID),
                Integer.class
        );
        Integer resolvedManagerId = (managerId == null || managerId <= 0)
                ? jdbcTemplate.queryForObject(
                // Fallback: bierzemy dowolnego pracownika, jesli klient nie podal managerId.
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_EMPLOYEE_ID),
                Integer.class
            )
                : managerId;

        if (resolvedManagerId == null || resolvedManagerId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book shop: manager does not exist");
        }

        int nextBookShopId = (maxBookShopId == null ? 0 : maxBookShopId) + 1;
        int nextOpeningHoursId = (maxOpeningHoursId == null ? 0 : maxOpeningHoursId) + 1;
        int firstBookShopIdToInsert = (requestedId == null || requestedId <= 0) ? nextBookShopId : requestedId;
        List<Integer> insertedBookShopIds = new ArrayList<>();
        List<Integer> insertedOpeningHoursIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentBookShopId = firstBookShopIdToInsert + i;
                int currentOpeningHoursId = nextOpeningHoursId + i;

                Instant start = Instant.now();
                int createdBookShop = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOKSHOP),
                        currentBookShopId,
                        shopName + "_" + currentBookShopId,
                        address,
                        email,
                        resolvedManagerId
                );

                int createdOpeningHours = jdbcTemplate.execute((Connection connection) -> {
                    PreparedStatement statement = connection.prepareStatement(
                            queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOKSHOP_OPENING_HOURS)
                    );
                    // Kolejnosc parametrow musi odpowiadac zapytaniu:
                    // 1=id godzin, 2..15=pary otwarcie/zamkniecie dla dni tygodnia, 16=bookshopid.
                    statement.setInt(1, currentOpeningHoursId);
                    statement.setObject(2, Time.valueOf("08:00:00"), Types.TIME);
                    statement.setObject(3, Time.valueOf("20:00:00"), Types.TIME);
                    statement.setObject(4, Time.valueOf("08:00:00"), Types.TIME);
                    statement.setObject(5, Time.valueOf("20:00:00"), Types.TIME);
                    statement.setObject(6, Time.valueOf("08:00:00"), Types.TIME);
                    statement.setObject(7, Time.valueOf("20:00:00"), Types.TIME);
                    statement.setObject(8, Time.valueOf("08:00:00"), Types.TIME);
                    statement.setObject(9, Time.valueOf("20:00:00"), Types.TIME);
                    statement.setObject(10, Time.valueOf("08:00:00"), Types.TIME);
                    statement.setObject(11, Time.valueOf("20:00:00"), Types.TIME);
                    statement.setObject(12, Time.valueOf("09:00:00"), Types.TIME);
                    statement.setObject(13, Time.valueOf("18:00:00"), Types.TIME);
                    statement.setObject(14, Time.valueOf("10:00:00"), Types.TIME);
                    statement.setObject(15, Time.valueOf("16:00:00"), Types.TIME);
                    statement.setInt(16, currentBookShopId);
                    return statement.executeUpdate();
                });

                int updatedBookShop = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.UPDATE_BOOKSHOP_OPENING_HOURS_ID),
                        currentOpeningHoursId,
                        currentBookShopId
                );
                Instant end = Instant.now();

                if (createdBookShop != 1 || createdOpeningHours != 1 || updatedBookShop != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert book shop failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedBookShopIds.add(currentBookShopId);
                insertedOpeningHoursIds.add(currentOpeningHoursId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 3,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "bookshop_ids", insertedBookShopIds,
                    "opening_hours_ids", insertedOpeningHoursIds,
                    "manager_id", resolvedManagerId,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book shop: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book shop: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
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
        validateUserRegistration(name, surname, email, login, passwordHash);

        Integer maxBookShopUserId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOKSHOP_USER_ID),
                Integer.class
        );
        Integer resolvedActivationStatusId = resolveRequiredReferenceId(
                engine,
                activationStatusId,
                UserPermissionQueryType.SELECT_ANY_ACTIVATION_STATUS_ID,
                "activation status"
        );
        // permissionId: id rekordu w bench.useraccountpermissions (FK w bench.useraccount.permissionsid).
        Integer resolvedPermissionId = resolveRequiredReferenceId(
                engine,
                permissionId,
                UserPermissionQueryType.SELECT_ANY_PERMISSION_ID,
                "permission"
        );

        int nextId = (maxBookShopUserId == null ? 0 : maxBookShopUserId) + 1;
        int firstIdToInsert = (requestedId == null || requestedId <= 0) ? nextId : requestedId;
        List<Integer> insertedIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentId = firstIdToInsert + i;
                Instant start = Instant.now();

                int createdBookShopUser = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOKSHOP_USER),
                        currentId,
                        name,
                        surname,
                        phoneNumber,
                        email,
                        mainBookShopId,
                        resolvedActivationStatusId
                );

                int createdUserCard = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_USER_CARD),
                    // usercard.id
                        currentId,
                    // usercard.cardidnumber
                        resolveCardIdNumber(cardIdNumber, currentId),
                    // usercard.userid (FK -> bookshopuser.id)
                        currentId,
                    // usercard.isactiveid (FK -> activationstatus.id)
                        resolvedActivationStatusId
                );

                int createdUserAccount = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_USER_ACCOUNT),
                    // useraccount.id
                        currentId,
                    // useraccount.login
                        resolveLogin(login, currentId),
                    // useraccount.passwordhash
                        resolvePasswordHash(passwordHash, currentId),
                    // useraccount.userid (FK -> bookshopuser.id)
                        currentId,
                    // useraccount.permissionsid (FK -> useraccountpermissions.id)
                        resolvedPermissionId
                );

                Instant end = Instant.now();

                if (createdBookShopUser != 1 || createdUserCard != 1 || createdUserAccount != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert user registration failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedIds.add(currentId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 4,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "user_ids", insertedIds,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs),
                    "activation_status_id", resolvedActivationStatusId,
                    "permission_id", resolvedPermissionId
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create user registration: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create user registration: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> read(DatabaseEngine engine, int id) {
        try {
            Instant start = Instant.now();
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.READ),
                    id
            );
            Instant end = Instant.now();

            logSingle(engine, CsvCrudLogger.CrudType.READ, readOperationCounter.getAndIncrement(), start, end);

            return Map.of(
                    "status", "read",
                    "engine", engine.propertyValue(),
                    "data", row
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }
    }

    @Override
    public Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details) {
        validatePermission(permission);

        Instant start = Instant.now();
        int affectedRows = jdbcTemplate.update(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.UPDATE),
                permission,
                details,
                id
        );
        Instant end = Instant.now();

        if (affectedRows != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }

        logSingle(engine, CsvCrudLogger.CrudType.UPDATE, updateOperationCounter.getAndIncrement(), start, end);

        return Map.of(
                "status", "updated",
                "engine", engine.propertyValue(),
                "id", id,
                "permission", permission,
                "details", details
        );
    }

    @Override
    public Map<String, Object> delete(DatabaseEngine engine, int id) {
        Instant start = Instant.now();
        int affectedRows = jdbcTemplate.update(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.DELETE),
                id
        );
        Instant end = Instant.now();

        if (affectedRows != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }

        logSingle(engine, CsvCrudLogger.CrudType.DELETE, deleteOperationCounter.getAndIncrement(), start, end);

        return Map.of(
                "status", "deleted",
                "engine", engine.propertyValue(),
                "id", id
        );
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
        // Pobierz najnowsze IDs
        Integer maxBookId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOK_ID),
                Integer.class
        );
        Integer maxOfferingId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOKSHOP_OFFERING_ID),
                Integer.class
        );
        Integer resolvedBookShopId = (bookShopId == null || bookShopId <= 0)
                ? jdbcTemplate.queryForObject(
                // Fallback: gdy brak shopId, wybieramy dowolny istniejacy sklep.
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_ID),
                Integer.class
            )
                : bookShopId;

        int nextBookId = (maxBookId == null ? 0 : maxBookId) + 1;
        int nextOfferingId = (maxOfferingId == null ? 0 : maxOfferingId) + 1;
        int firstBookIdToInsert = (requestedBookId == null || requestedBookId <= 0) ? nextBookId : requestedBookId;
        int firstOfferingIdToInsert = (requestedOfferingId == null || requestedOfferingId <= 0) ? nextOfferingId : requestedOfferingId;

        List<Integer> insertedBookIds = new ArrayList<>();
        List<Integer> insertedOfferingIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentBookId = firstBookIdToInsert + i;
                int currentOfferingId = firstOfferingIdToInsert + i;

                Instant start = Instant.now();
                int createdBook = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOK),
                        currentBookId,
                        author,
                        title + "_" + currentBookId,
                        resolvedBookShopId
                );

                int createdOffering = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOKSHOP_OFFERING),
                    // Placeholdery: 1=offering.id, 2=bookid, 3=bookshopid.
                        currentOfferingId,
                        currentBookId,
                        resolvedBookShopId
                );
                Instant end = Instant.now();

                if (createdBook != 1 || createdOffering != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert book with offering failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedBookIds.add(currentBookId);
                insertedOfferingIds.add(currentOfferingId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 5,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "book_ids", insertedBookIds,
                    "offering_ids", insertedOfferingIds,
                    "bookshop_id", resolvedBookShopId,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book with offering: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book with offering: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> createBookReservation(
            DatabaseEngine engine,
            Integer requestedReservationId,
            Integer bookId,
            Integer userId
    ) {
        // Pobierz najnowsze IDs
        Integer maxReservationId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOK_RESERVATION_ID),
                Integer.class
        );
        Integer resolvedBookId = (bookId == null || bookId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOK_ID),
                Integer.class
        )
                : bookId;
        Integer resolvedUserId = (userId == null || userId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_USER_ID),
                Integer.class
        )
                : userId;

        int nextReservationId = (maxReservationId == null ? 0 : maxReservationId) + 1;
        int firstReservationIdToInsert = (requestedReservationId == null || requestedReservationId <= 0) ? nextReservationId : requestedReservationId;

        List<Integer> insertedReservationIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentReservationId = firstReservationIdToInsert + i;

                Instant start = Instant.now();
                int createdReservation = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BOOK_RESERVATION),
                        currentReservationId,
                        resolvedBookId,
                        resolvedUserId,
                        java.sql.Date.valueOf(java.time.LocalDate.now())
                );
                Instant end = Instant.now();

                if (createdReservation != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert book reservation failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedReservationIds.add(currentReservationId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 6,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "reservation_ids", insertedReservationIds,
                    "book_id", resolvedBookId,
                    "user_id", resolvedUserId,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book reservation: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create book reservation: " + ex.getMessage());
        }
    }

    @PreDestroy
    public void closeLogger() {
        if (csvLogger == null) {
            return;
        }

        try {
            csvLogger.close();
        } catch (IOException e) {
            System.err.println("Nie mozna zamknac loggera CSV: " + e.getMessage());
        }
    }

    private void validatePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permission is required");
        }
    }

    private void validateRentalMethod(String method) {
        if (method == null || method.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "method is required");
        }
    }

    private void validateBookShop(String shopName, String address, String email) {
        if (shopName == null || shopName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "shopName is required");
        }
        if (address == null || address.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "address is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
    }

    private void validateUserRegistration(String name, String surname, String email, String login, String passwordHash) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (surname == null || surname.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "surname is required");
        }
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "email is required");
        }
        if (login == null || login.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "login is required");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passwordHash is required");
        }
    }

    private Integer resolveRequiredReferenceId(
            DatabaseEngine engine,
            Integer requestedId,
            UserPermissionQueryType fallbackQueryType,
            String label
    ) {
        if (requestedId != null && requestedId > 0) {
            return requestedId;
        }

        Integer resolvedId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, fallbackQueryType),
                Integer.class
        );

        if (resolvedId == null || resolvedId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create user registration: missing " + label);
        }

        return resolvedId;
    }

    private String resolveCardIdNumber(String cardIdNumber, int currentId) {
        if (cardIdNumber != null && !cardIdNumber.isBlank()) {
            return cardIdNumber;
        }
        return String.format(Locale.US, "CARD-%06d", currentId);
    }

    private String resolveLogin(String login, int currentId) {
        if (login != null && !login.isBlank()) {
            return login;
        }
        return String.format(Locale.US, "user%06d", currentId);
    }

    private String resolvePasswordHash(String passwordHash, int currentId) {
        if (passwordHash != null && !passwordHash.isBlank()) {
            return passwordHash;
        }
        return String.format(Locale.US, "sha256$%032x$benchmark", currentId);
    }

    private void logCreate(DatabaseEngine engine, List<Instant> starts, List<Instant> ends, double averageDurationMs) {
        try {
            getLogger(engine).logCreate(
                    createOperationCounter.getAndIncrement(),
                    starts.get(0), ends.get(0),
                    starts.get(1), ends.get(1),
                    starts.get(2), ends.get(2),
                    averageDurationMs
            );
        } catch (IOException e) {
            System.err.println("Blad logowania CREATE do CSV: " + e.getMessage());
        }
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
        // CREATE 7: komplet FK potrzebnych do bench.bookrental.
        Integer maxRentalId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_RENTAL_ID),
                Integer.class
        );
        Integer resolvedBookId = (bookId == null || bookId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOK_ID),
                Integer.class
        ) : bookId;
        Integer resolvedUserId = (userId == null || userId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_USER_ID),
                Integer.class
        ) : userId;
        Integer resolvedEmployeeId = (employeeId == null || employeeId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_EMPLOYEE_ID),
                Integer.class
        ) : employeeId;
        Integer resolvedShopId = (shopId == null || shopId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_ID),
                Integer.class
        ) : shopId;
        Integer resolvedRentalMethodId = (rentalMethodId == null || rentalMethodId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_RENTAL_METHOD_ID),
                Integer.class
        ) : rentalMethodId;

        int nextRentalId = (maxRentalId == null ? 0 : maxRentalId) + 1;
        int firstRentalIdToInsert = (requestedRentalId == null || requestedRentalId <= 0) ? nextRentalId : requestedRentalId;

        List<Integer> insertedRentalIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentRentalId = firstRentalIdToInsert + i;

                Instant start = Instant.now();
                int createdRental = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_RENTAL_FULL),
                    // 1=id, 2=bookid, 3=userid, 4=employeeid, 5=bookshopid, 6=rentalMethodId,
                    // 7=startdate, 8=enddate (isreturned jest stale 0 w SQL).
                        currentRentalId,
                        resolvedBookId,
                        resolvedUserId,
                        resolvedEmployeeId,
                        resolvedShopId,
                        resolvedRentalMethodId,
                        java.sql.Date.valueOf(java.time.LocalDate.now()),
                        java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(7))
                );
                Instant end = Instant.now();

                if (createdRental != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert rental failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedRentalIds.add(currentRentalId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("status", "created");
            result.put("operation", 7);
            result.put("engine", engine.propertyValue());
            result.put("executions", CREATE_REPETITIONS);
            result.put("rental_ids", insertedRentalIds);
            result.put("book_id", resolvedBookId);
            result.put("user_id", resolvedUserId);
            result.put("employee_id", resolvedEmployeeId);
            result.put("shop_id", resolvedShopId);
            result.put("rental_method_id", resolvedRentalMethodId);
            result.put("average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs));
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create rental: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create rental: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> createConditionalRental(
            DatabaseEngine engine,
            Integer requestedRentalId
    ) {
        // CREATE 8: najpierw sprawdzamy warunek biznesowy,
        // czy istnieje aktywny uzytkownik z ksiazka w jego sklepie.
        Integer resolvedUserId = null;
        try {
            resolvedUserId = jdbcTemplate.queryForObject(
                    queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ACTIVE_USER_WITH_BOOK_IN_SHOP),
                    Integer.class
            );
        } catch (Exception e) {
            // Fallback: jesli brak wyniku dla warunku, bierzemy dowolnego uzytkownika.
            resolvedUserId = jdbcTemplate.queryForObject(
                    queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_USER_ID),
                    Integer.class
            );
        }

        Integer maxRentalId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_RENTAL_ID),
                Integer.class
        );
        Integer resolvedBookId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOK_ID),
                Integer.class
        );
        Integer resolvedEmployeeId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_EMPLOYEE_ID),
                Integer.class
        );
        Integer resolvedShopId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_ID),
                Integer.class
        );
        Integer resolvedRentalMethodId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_RENTAL_METHOD_ID),
                Integer.class
        );

        int nextRentalId = (maxRentalId == null ? 0 : maxRentalId) + 1;
        int firstRentalIdToInsert = (requestedRentalId == null || requestedRentalId <= 0) ? nextRentalId : requestedRentalId;

        List<Integer> insertedRentalIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentRentalId = firstRentalIdToInsert + i;

                Instant start = Instant.now();
                int createdRental = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_RENTAL_CONDITIONAL),
                    // Parametry takie same jak CREATE_RENTAL_FULL.
                        currentRentalId,
                        resolvedBookId,
                        resolvedUserId,
                        resolvedEmployeeId,
                        resolvedShopId,
                        resolvedRentalMethodId,
                        java.sql.Date.valueOf(java.time.LocalDate.now()),
                        java.sql.Date.valueOf(java.time.LocalDate.now().plusDays(7))
                );
                Instant end = Instant.now();

                if (createdRental != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert conditional rental failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedRentalIds.add(currentRentalId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 8,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "rental_ids", insertedRentalIds,
                    "user_id", resolvedUserId,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create conditional rental: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create conditional rental: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> createBatchSupplyEvent(
            DatabaseEngine engine,
            Integer startBookId,
            Integer numberOfBooks,
            Integer shopId
    ) {
        // CREATE 9: hurtowe dodanie ksiazek do jednego sklepu.
        Integer resolvedShopId = (shopId == null || shopId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_ANY_BOOKSHOP_ID),
                Integer.class
        ) : shopId;

        int firstBookIdToInsert = (startBookId == null || startBookId <= 0)
                ? jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_BOOK_ID),
                Integer.class
        ) + 1 : startBookId;
        int numberOfBooksPerRep = (numberOfBooks == null || numberOfBooks <= 0) ? 5 : numberOfBooks;

        List<Integer> insertedBookIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int rep = 0; rep < CREATE_REPETITIONS; rep++) {
                Instant start = Instant.now();
                int booksCreatedInRep = 0;

                for (int b = 0; b < numberOfBooksPerRep; b++) {
                    // Unikalne id w ramach calego benchmarku: przesuniecie po rep + indeks lokalny.
                    int currentBookId = firstBookIdToInsert + rep * numberOfBooksPerRep + b;
                    int createdBook = jdbcTemplate.update(
                            queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE_BATCH_SUPPLY),
                            currentBookId,
                            "Autor Batch " + currentBookId,
                            "Tytuł Batch " + currentBookId,
                            resolvedShopId
                    );

                    if (createdBook == 1) {
                        booksCreatedInRep++;
                        insertedBookIds.add(currentBookId);
                    }
                }

                Instant end = Instant.now();

                if (booksCreatedInRep == 0) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert batch books failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "operation", 9,
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "books_created", insertedBookIds.size(),
                    "book_ids", insertedBookIds,
                    "shop_id", resolvedShopId,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs)
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (DataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create batch supply: " + ex.getMostSpecificCause().getMessage());
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create batch supply: " + ex.getMessage());
        }
    }

    private void logSingle(DatabaseEngine engine, CsvCrudLogger.CrudType crudType, int operationNumber, Instant start, Instant end) {
        try {
            getLogger(engine).log(crudType, operationNumber, start, end);
        } catch (IOException e) {
            System.err.println("Blad logowania do CSV: " + e.getMessage());
        }
    }

    private synchronized CsvCrudLogger getLogger(DatabaseEngine engine) throws IOException {
        if (csvLogger == null) {
            csvLogger = new CsvCrudLogger(engine.propertyValue(), engineResolver.resolveDatasetSize());
        }
        return csvLogger;
    }
}
