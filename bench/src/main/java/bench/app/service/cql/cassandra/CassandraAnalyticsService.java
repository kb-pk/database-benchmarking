package bench.app.service.cql.cassandra;

import bench.app.model.common.ActiveUser;
import bench.app.model.common.BookRentalRanking;
import bench.app.model.common.BookRentalCloseOverdueResult;
import bench.app.model.common.BookShopOpeningHoursUpdateResult;
import bench.app.model.common.EmployeeRentalCount;
import bench.app.model.common.EmployeeShopAssignmentUpdateResult;
import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserActivationBulkUpdateResult;
import bench.app.model.common.UserGroupShopTransferResult;
import bench.app.model.common.UserPermissionCreateResult;
import bench.app.model.common.UserPermissionUpdateResult;
import bench.app.model.common.UserReservationCount;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

@Service
public class CassandraAnalyticsService {
    private final CqlSession cassandraSession;

    public CassandraAnalyticsService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public List<ActiveUser> getActiveUsersByShopId(UUID shopId) {
        List<ActiveUser> result = new ArrayList<>();
        ResultSet rs = cassandraSession.execute("SELECT user_id, name, surname, phone_number, email, status, main_book_shop_id FROM users");

        for (Row row : rs) {
            if (row.isNull("main_book_shop_id")) {
                continue;
            }
            UUID userShopId = row.getUuid("main_book_shop_id");
            if (!shopId.equals(userShopId)) {
                continue;
            }

            String status = row.getString("status");
            if (status == null || !"ACTIVE".equalsIgnoreCase(status.trim())) {
                continue;
            }

            result.add(new ActiveUser(
                    uuidToPositiveLong(row.getUuid("user_id")),
                    row.getString("name"),
                    row.getString("surname"),
                    row.getString("phone_number"),
                    row.getString("email"),
                    status
            ));
        }

        result.sort((a, b) -> Long.compare(a.id(), b.id()));
        return result;
    }

    public List<UserReservationCount> getTopUsersByReservationCountGlobal() {
        Map<UUID, Long> reservationCounts = new HashMap<>();
        ResultSet reservations = cassandraSession.execute("SELECT user_id FROM reservations_by_user");
        for (Row row : reservations) {
            UUID userId = row.getUuid("user_id");
            reservationCounts.merge(userId, 1L, Long::sum);
        }

        Map<UUID, UserDetails> usersById = readUsersById();
        List<UserReservationCount> result = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : reservationCounts.entrySet()) {
            UserDetails user = usersById.get(entry.getKey());
            if (user == null) {
                continue;
            }

            result.add(new UserReservationCount(
                    uuidToPositiveLong(entry.getKey()),
                    user.name,
                    user.surname,
                    entry.getValue()
            ));
        }

        result.sort((a, b) -> {
            int byCount = Long.compare(b.reservationCount(), a.reservationCount());
            if (byCount != 0) {
                return byCount;
            }
            return Long.compare(a.userId(), b.userId());
        });
        return result;
    }

    public List<EmployeeRentalCount> getEmployeeRentalCountsGlobal() {
        Map<UUID, Long> rentalCounts = new HashMap<>();
        ResultSet rentals = cassandraSession.execute("SELECT employee_id FROM rentals_by_shop");
        for (Row row : rentals) {
            UUID employeeId = row.getUuid("employee_id");
            rentalCounts.merge(employeeId, 1L, Long::sum);
        }

        Map<UUID, EmployeeDetails> employeesById = readEmployeesById();
        List<EmployeeRentalCount> result = new ArrayList<>();
        for (Map.Entry<UUID, Long> entry : rentalCounts.entrySet()) {
            EmployeeDetails employee = employeesById.get(entry.getKey());
            if (employee == null) {
                continue;
            }

            result.add(new EmployeeRentalCount(
                    uuidToPositiveLong(entry.getKey()),
                    employee.name,
                    employee.surname,
                    entry.getValue()
            ));
        }

        result.sort((a, b) -> {
            int byCount = Long.compare(b.rentalCount(), a.rentalCount());
            if (byCount != 0) {
                return byCount;
            }
            return Long.compare(a.employeeId(), b.employeeId());
        });
        return result;
    }

    public List<BookRentalRanking> getBookRentalRankingByShop(UUID shopId) {
        SimpleStatement statement = SimpleStatement.builder(
                        "SELECT book_id FROM rentals_by_shop WHERE shop_id = ?")
                .addPositionalValue(shopId)
                .build();

        Map<UUID, Long> rentalCountsByBook = new HashMap<>();
        ResultSet rentals = cassandraSession.execute(statement);
        for (Row row : rentals) {
            UUID bookId = row.getUuid("book_id");
            rentalCountsByBook.merge(bookId, 1L, Long::sum);
        }

        Map<UUID, BookDetails> booksById = readBooksByShop(shopId);

        List<Map.Entry<UUID, Long>> sorted = new ArrayList<>(rentalCountsByBook.entrySet());
        sorted.sort((a, b) -> {
            int byCount = Long.compare(b.getValue(), a.getValue());
            if (byCount != 0) {
                return byCount;
            }
            return a.getKey().compareTo(b.getKey());
        });

        List<BookRentalRanking> result = new ArrayList<>();
        long currentRank = 0;
        long previousCount = Long.MIN_VALUE;
        for (int i = 0; i < sorted.size(); i++) {
            UUID bookId = sorted.get(i).getKey();
            long count = sorted.get(i).getValue();

            if (count != previousCount) {
                currentRank = i + 1;
                previousCount = count;
            }

            BookDetails details = booksById.get(bookId);
            String title = details != null ? details.title : "unknown";
            String author = details != null ? details.author : "unknown";

            result.add(new BookRentalRanking(
                    uuidToPositiveLong(bookId),
                    title,
                    author,
                    count,
                    currentRank
            ));
        }

        return result;
    }

    public List<EngagedUser> getEngagedUsersByPeriod(LocalDate fromDate, LocalDate toDate) {
        Set<UUID> usersWithReservations = new HashSet<>();
        ResultSet reservations = cassandraSession.execute("SELECT user_id, when_reserved FROM reservations_by_user");
        for (Row row : reservations) {
            LocalDate whenReserved = row.getLocalDate("when_reserved");
            if (isWithinRange(whenReserved, fromDate, toDate)) {
                usersWithReservations.add(row.getUuid("user_id"));
            }
        }

        Set<UUID> usersWithRentals = new HashSet<>();
        ResultSet rentals = cassandraSession.execute("SELECT user_id, start_date FROM rentals_by_user");
        for (Row row : rentals) {
            LocalDate startDate = row.getLocalDate("start_date");
            if (isWithinRange(startDate, fromDate, toDate)) {
                usersWithRentals.add(row.getUuid("user_id"));
            }
        }

        usersWithReservations.retainAll(usersWithRentals);

        Map<UUID, UserDetails> usersById = readUsersById();
        List<UUID> sortedIds = new ArrayList<>(usersWithReservations);
        sortedIds.sort(UUID::compareTo);

        List<EngagedUser> result = new ArrayList<>();
        for (UUID userId : sortedIds) {
            UserDetails user = usersById.get(userId);
            if (user == null) {
                continue;
            }

            result.add(new EngagedUser(
                    uuidToPositiveLong(userId),
                    user.name,
                    user.surname,
                    user.phoneNumber,
                    user.email
            ));
        }

        return result;
    }

    public UserActivationBulkUpdateResult setUsersInactiveIfNoOpenRentalOrReservation(boolean restoreAfterUpdate) {
        Set<UUID> usersWithOpenRentals = new HashSet<>();
        ResultSet rentals = cassandraSession.execute("SELECT user_id, is_returned FROM rentals_by_user");
        for (Row row : rentals) {
            UUID userId = row.getUuid("user_id");
            Boolean isReturned = row.getBoolean("is_returned");
            if (!Boolean.TRUE.equals(isReturned)) {
                usersWithOpenRentals.add(userId);
            }
        }

        Set<UUID> usersWithReservations = new HashSet<>();
        ResultSet reservations = cassandraSession.execute("SELECT user_id FROM reservations_by_user");
        for (Row row : reservations) {
            usersWithReservations.add(row.getUuid("user_id"));
        }

        List<UserStatusSnapshot> matchedUsers = new ArrayList<>();
        ResultSet users = cassandraSession.execute("SELECT user_id, status, login FROM users");
        for (Row row : users) {
            UUID userId = row.getUuid("user_id");
            String status = row.getString("status");
            String login = row.getString("login");

            if (status == null || !"ACTIVE".equalsIgnoreCase(status.trim())) {
                continue;
            }
            if (usersWithOpenRentals.contains(userId) || usersWithReservations.contains(userId)) {
                continue;
            }

            matchedUsers.add(new UserStatusSnapshot(userId, status, login));
        }

        int updatedUsers = 0;
        for (UserStatusSnapshot user : matchedUsers) {
            cassandraSession.execute(
                    SimpleStatement.builder("UPDATE users SET status = ? WHERE user_id = ?")
                            .addPositionalValue("INACTIVE")
                            .addPositionalValue(user.userId)
                            .build()
            );

            if (user.login != null && !user.login.isBlank()) {
                cassandraSession.execute(
                        SimpleStatement.builder("UPDATE user_credentials_by_login SET status = ? WHERE login = ?")
                                .addPositionalValue("INACTIVE")
                                .addPositionalValue(user.login)
                                .build()
                );
            }
            updatedUsers++;
        }

        int restoredUsers = 0;
        if (restoreAfterUpdate) {
            for (UserStatusSnapshot user : matchedUsers) {
                cassandraSession.execute(
                        SimpleStatement.builder("UPDATE users SET status = ? WHERE user_id = ?")
                                .addPositionalValue(user.previousStatus)
                                .addPositionalValue(user.userId)
                                .build()
                );

                if (user.login != null && !user.login.isBlank()) {
                    cassandraSession.execute(
                            SimpleStatement.builder("UPDATE user_credentials_by_login SET status = ? WHERE login = ?")
                                    .addPositionalValue(user.previousStatus)
                                    .addPositionalValue(user.login)
                                    .build()
                    );
                }
                restoredUsers++;
            }
        }

        return new UserActivationBulkUpdateResult(
                -1,
                matchedUsers.size(),
                updatedUsers,
                restoredUsers,
                restoreAfterUpdate
        );
    }

    public UserPermissionUpdateResult updateUserPermissions(UUID userId, long permissionsId, boolean restoreAfterUpdate) {
        Row userRow = cassandraSession.execute(
                        SimpleStatement.builder("SELECT user_id, permissions FROM users WHERE user_id = ?")
                                .addPositionalValue(userId)
                                .build())
                .one();

        if (userRow == null) {
            throw new IllegalArgumentException("Nie znaleziono użytkownika userId=" + userId);
        }

        Set<String> previousPermissions = userRow.getSet("permissions", String.class);
        Set<String> safePreviousPermissions = previousPermissions == null ? Collections.emptySet() : new HashSet<>(previousPermissions);

        List<Set<String>> profiles = readPermissionProfiles();
        if (profiles.isEmpty()) {
            profiles = new ArrayList<>();
            profiles.add(Collections.emptySet());
        }

        if (permissionsId < 1 || permissionsId > profiles.size()) {
            throw new IllegalArgumentException("Nie znaleziono profilu uprawnień permissionsId=" + permissionsId);
        }

        Set<String> targetPermissions = profiles.get((int) permissionsId - 1);
        int previousPermissionsId = findProfileId(safePreviousPermissions, profiles);

        cassandraSession.execute(
                SimpleStatement.builder("UPDATE users SET permissions = ? WHERE user_id = ?")
                        .addPositionalValue(targetPermissions)
                        .addPositionalValue(userId)
                        .build()
        );

        long finalPermissionsId = permissionsId;
        if (restoreAfterUpdate) {
            cassandraSession.execute(
                    SimpleStatement.builder("UPDATE users SET permissions = ? WHERE user_id = ?")
                            .addPositionalValue(safePreviousPermissions)
                            .addPositionalValue(userId)
                            .build()
            );
            finalPermissionsId = previousPermissionsId;
        }

        return new UserPermissionUpdateResult(
                uuidToPositiveLong(userId),
                previousPermissionsId,
                permissionsId,
                finalPermissionsId,
                restoreAfterUpdate,
                1
        );
    }

    public UserPermissionCreateResult createPermission(String permission, String details, boolean restoreAfterCreate) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("permission nie może być puste");
        }

        UUID userId = findFirstActiveUserId();
        if (userId == null) {
            throw new IllegalArgumentException("Nie znaleziono aktywnego użytkownika do C1");
        }

        Row userRow = cassandraSession.execute(
                        SimpleStatement.builder("SELECT permissions FROM users WHERE user_id = ?")
                                .addPositionalValue(userId)
                                .build())
                .one();
        Set<String> previousPermissions = userRow == null ? Collections.emptySet() : userRow.getSet("permissions", String.class);
        Set<String> safePreviousPermissions = previousPermissions == null ? Collections.emptySet() : new HashSet<>(previousPermissions);

        Set<String> updatedPermissions = new HashSet<>(safePreviousPermissions);
        updatedPermissions.add(permission);

        cassandraSession.execute(
                SimpleStatement.builder("UPDATE users SET permissions = ? WHERE user_id = ?")
                        .addPositionalValue(updatedPermissions)
                        .addPositionalValue(userId)
                        .build()
        );

        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            cassandraSession.execute(
                    SimpleStatement.builder("UPDATE users SET permissions = ? WHERE user_id = ?")
                            .addPositionalValue(safePreviousPermissions)
                            .addPositionalValue(userId)
                            .build()
            );
            existsAfterOperation = false;
        }

        return new UserPermissionCreateResult(
                uuidToPositiveLong(userId),
                permission,
                details,
                restoreAfterCreate,
                existsAfterOperation,
                1,
                restoreAfterCreate ? 1 : 0
        );
    }

    public BookShopOpeningHoursUpdateResult updateMondayOpeningHours(
            UUID shopId,
            LocalTime opensAtMonday,
            LocalTime closesAtMonday,
            boolean restoreAfterUpdate
    ) {
        Row row = cassandraSession.execute(
                        SimpleStatement.builder("SELECT opens_at_monday, closes_at_monday FROM bookshops WHERE shop_id = ?")
                                .addPositionalValue(shopId)
                                .build())
                .one();

        if (row == null) {
            throw new IllegalArgumentException("Nie znaleziono sklepu shopId=" + shopId);
        }

        LocalTime previousOpens = row.getLocalTime("opens_at_monday");
        LocalTime previousCloses = row.getLocalTime("closes_at_monday");

        cassandraSession.execute(
                SimpleStatement.builder("UPDATE bookshops SET opens_at_monday = ?, closes_at_monday = ? WHERE shop_id = ?")
                        .addPositionalValue(opensAtMonday)
                        .addPositionalValue(closesAtMonday)
                        .addPositionalValue(shopId)
                        .build()
        );

        LocalTime finalOpens = opensAtMonday;
        LocalTime finalCloses = closesAtMonday;
        if (restoreAfterUpdate) {
            cassandraSession.execute(
                    SimpleStatement.builder("UPDATE bookshops SET opens_at_monday = ?, closes_at_monday = ? WHERE shop_id = ?")
                            .addPositionalValue(previousOpens)
                            .addPositionalValue(previousCloses)
                            .addPositionalValue(shopId)
                            .build()
            );
            finalOpens = previousOpens;
            finalCloses = previousCloses;
        }

        return new BookShopOpeningHoursUpdateResult(
                uuidToPositiveLong(shopId),
                previousOpens,
                previousCloses,
                opensAtMonday,
                closesAtMonday,
                finalOpens,
                finalCloses,
                restoreAfterUpdate,
                1
        );
    }

    public EmployeeShopAssignmentUpdateResult reassignEmployeeToShop(
            UUID employeeId,
            UUID newShopId,
            boolean restoreAfterUpdate
    ) {
        Row targetShop = cassandraSession.execute(
                        SimpleStatement.builder("SELECT shop_id FROM bookshops WHERE shop_id = ?")
                                .addPositionalValue(newShopId)
                                .build())
                .one();
        if (targetShop == null) {
            throw new IllegalArgumentException("Nie znaleziono sklepu shopId=" + newShopId);
        }

        Row employeeRow = cassandraSession.execute(
                        SimpleStatement.builder("""
                                SELECT primary_book_shop_id, employee_id, name, surname, phone_number, email,
                                       birth_date, started_at, primary_business_role
                                FROM employees_by_shop
                                WHERE employee_id = ?
                                ALLOW FILTERING
                                """)
                                .addPositionalValue(employeeId)
                                .build())
                .one();
        if (employeeRow == null) {
            throw new IllegalArgumentException("Nie znaleziono pracownika employeeId=" + employeeId);
        }

        UUID previousShopId = employeeRow.getUuid("primary_book_shop_id");
        if (previousShopId == null) {
            throw new IllegalArgumentException("Pracownik nie ma przypisanego sklepu employeeId=" + employeeId);
        }

        if (previousShopId.equals(newShopId)) {
            return new EmployeeShopAssignmentUpdateResult(
                    uuidToPositiveLong(employeeId),
                    uuidToPositiveLong(previousShopId),
                    uuidToPositiveLong(newShopId),
                    uuidToPositiveLong(previousShopId),
                    restoreAfterUpdate,
                    1
            );
        }

        insertEmployeeRow(newShopId, employeeRow);
        deleteEmployeeRow(previousShopId, employeeId);

        UUID finalShopId = newShopId;
        if (restoreAfterUpdate) {
            insertEmployeeRow(previousShopId, employeeRow);
            deleteEmployeeRow(newShopId, employeeId);
            finalShopId = previousShopId;
        }

        return new EmployeeShopAssignmentUpdateResult(
                uuidToPositiveLong(employeeId),
                uuidToPositiveLong(previousShopId),
                uuidToPositiveLong(newShopId),
                uuidToPositiveLong(finalShopId),
                restoreAfterUpdate,
                1
        );
    }

    public BookRentalCloseOverdueResult closeOverdueRentals(int daysThreshold, boolean restoreAfterUpdate) {
        if (daysThreshold <= 0) {
            throw new IllegalArgumentException("daysThreshold musi być większe od 0");
        }

        LocalDate cutoffDate = LocalDate.now().minusDays(daysThreshold);
        List<RentalSnapshot> matchedRentals = new ArrayList<>();

        ResultSet rentals = cassandraSession.execute(
                "SELECT user_id, start_date, rental_id, shop_id, is_returned, end_date FROM rentals_by_user"
        );
        for (Row row : rentals) {
            LocalDate startDate = row.getLocalDate("start_date");
            Boolean isReturned = row.getBoolean("is_returned");
            if (startDate == null || !startDate.isBefore(cutoffDate)) {
                continue;
            }
            if (Boolean.TRUE.equals(isReturned)) {
                continue;
            }

            matchedRentals.add(new RentalSnapshot(
                    row.getUuid("user_id"),
                    row.getUuid("shop_id"),
                    startDate,
                    row.getUuid("rental_id"),
                    isReturned,
                    row.getLocalDate("end_date")
            ));
        }

        int closedRentals = 0;
        LocalDate now = LocalDate.now();
        for (RentalSnapshot rental : matchedRentals) {
            updateRentalState(rental, true, now);
            closedRentals++;
        }

        int restoredRentals = 0;
        if (restoreAfterUpdate) {
            for (RentalSnapshot rental : matchedRentals) {
                updateRentalState(rental, rental.previousReturned, rental.previousEndDate);
                restoredRentals++;
            }
        }

        return new BookRentalCloseOverdueResult(
                daysThreshold,
                matchedRentals.size(),
                closedRentals,
                restoredRentals,
                restoreAfterUpdate
        );
    }

    public UserGroupShopTransferResult transferUserGroup(
            UUID sourceShopId,
            UUID targetShopId,
            int maxUsers,
            boolean restoreAfterUpdate
    ) {
        if (maxUsers <= 0) {
            throw new IllegalArgumentException("maxUsers musi być większe od 0");
        }
        if (sourceShopId.equals(targetShopId)) {
            throw new IllegalArgumentException("sourceShopId i targetShopId muszą być różne");
        }

        if (!shopExists(sourceShopId)) {
            throw new IllegalArgumentException("Nie znaleziono sourceShopId=" + sourceShopId);
        }
        if (!shopExists(targetShopId)) {
            throw new IllegalArgumentException("Nie znaleziono targetShopId=" + targetShopId);
        }

        ResultSet sourceUsers = cassandraSession.execute(
                SimpleStatement.builder("SELECT user_id FROM users WHERE main_book_shop_id = ? LIMIT ? ALLOW FILTERING")
                        .addPositionalValue(sourceShopId)
                        .addPositionalValue(maxUsers)
                        .build()
        );

        List<UUID> movedUserIds = new ArrayList<>();
        for (Row row : sourceUsers) {
            UUID userId = row.getUuid("user_id");
            cassandraSession.execute(
                    SimpleStatement.builder("UPDATE users SET main_book_shop_id = ? WHERE user_id = ?")
                            .addPositionalValue(targetShopId)
                            .addPositionalValue(userId)
                            .build()
            );
            movedUserIds.add(userId);
        }

        int restoredUsers = 0;
        if (restoreAfterUpdate) {
            for (UUID userId : movedUserIds) {
                cassandraSession.execute(
                        SimpleStatement.builder("UPDATE users SET main_book_shop_id = ? WHERE user_id = ?")
                                .addPositionalValue(sourceShopId)
                                .addPositionalValue(userId)
                                .build()
                );
                restoredUsers++;
            }
        }

        return new UserGroupShopTransferResult(
                uuidToPositiveLong(sourceShopId),
                uuidToPositiveLong(targetShopId),
                maxUsers,
                movedUserIds.size(),
                restoredUsers,
                restoreAfterUpdate
        );
    }

    private Map<UUID, UserDetails> readUsersById() {
        Map<UUID, UserDetails> usersById = new LinkedHashMap<>();
        ResultSet users = cassandraSession.execute("SELECT user_id, name, surname, phone_number, email FROM users");
        for (Row row : users) {
            UUID id = row.getUuid("user_id");
            usersById.put(id, new UserDetails(
                    row.getString("name"),
                    row.getString("surname"),
                    row.getString("phone_number"),
                    row.getString("email")
            ));
        }
        return usersById;
    }

    private Map<UUID, EmployeeDetails> readEmployeesById() {
        Map<UUID, EmployeeDetails> employeesById = new HashMap<>();
        ResultSet employees = cassandraSession.execute("SELECT employee_id, name, surname FROM employees_by_shop");
        for (Row row : employees) {
            UUID employeeId = row.getUuid("employee_id");
            employeesById.putIfAbsent(employeeId, new EmployeeDetails(
                    row.getString("name"),
                    row.getString("surname")
            ));
        }
        return employeesById;
    }

    private Map<UUID, BookDetails> readBooksByShop(UUID shopId) {
        SimpleStatement statement = SimpleStatement.builder(
                        "SELECT book_id, title, author FROM books_by_shop WHERE shop_id = ?")
                .addPositionalValue(shopId)
                .build();

        Map<UUID, BookDetails> booksById = new HashMap<>();
        ResultSet books = cassandraSession.execute(statement);
        for (Row row : books) {
            booksById.put(row.getUuid("book_id"), new BookDetails(
                    row.getString("title"),
                    row.getString("author")
            ));
        }
        return booksById;
    }

    private UUID findFirstActiveUserId() {
        ResultSet users = cassandraSession.execute("SELECT user_id, status FROM users");
        for (Row row : users) {
            String status = row.getString("status");
            if (status != null && "ACTIVE".equalsIgnoreCase(status.trim())) {
                return row.getUuid("user_id");
            }
        }
        return null;
    }

    private List<Set<String>> readPermissionProfiles() {
        Map<String, Set<String>> profilesByKey = new TreeMap<>();
        ResultSet users = cassandraSession.execute("SELECT permissions FROM users");
        for (Row row : users) {
            Set<String> permissions = row.getSet("permissions", String.class);
            Set<String> safePermissions = permissions == null ? Collections.emptySet() : new HashSet<>(permissions);
            profilesByKey.putIfAbsent(normalizePermissions(safePermissions), safePermissions);
        }
        return new ArrayList<>(profilesByKey.values());
    }

    private int findProfileId(Set<String> permissions, List<Set<String>> profiles) {
        String key = normalizePermissions(permissions);
        for (int i = 0; i < profiles.size(); i++) {
            if (normalizePermissions(profiles.get(i)).equals(key)) {
                return i + 1;
            }
        }
        return profiles.size() + 1;
    }

    private String normalizePermissions(Set<String> permissions) {
        List<String> sorted = new ArrayList<>(permissions);
        sorted.sort(String::compareTo);
        return String.join("|", sorted);
    }

    private void insertEmployeeRow(UUID shopId, Row source) {
        cassandraSession.execute(
                SimpleStatement.builder("""
                        INSERT INTO employees_by_shop (
                            primary_book_shop_id, employee_id, name, surname, phone_number, email,
                            birth_date, started_at, primary_business_role
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)
                        .addPositionalValue(shopId)
                        .addPositionalValue(source.getUuid("employee_id"))
                        .addPositionalValue(source.getString("name"))
                        .addPositionalValue(source.getString("surname"))
                        .addPositionalValue(source.getString("phone_number"))
                        .addPositionalValue(source.getString("email"))
                        .addPositionalValue(source.getLocalDate("birth_date"))
                        .addPositionalValue(source.getLocalDate("started_at"))
                        .addPositionalValue(source.getString("primary_business_role"))
                        .build()
        );
    }

    private void deleteEmployeeRow(UUID shopId, UUID employeeId) {
        cassandraSession.execute(
                SimpleStatement.builder("DELETE FROM employees_by_shop WHERE primary_book_shop_id = ? AND employee_id = ?")
                        .addPositionalValue(shopId)
                        .addPositionalValue(employeeId)
                        .build()
        );
    }

    private void updateRentalState(RentalSnapshot rental, Boolean isReturned, LocalDate endDate) {
        cassandraSession.execute(
                SimpleStatement.builder("""
                        UPDATE rentals_by_user
                        SET is_returned = ?, end_date = ?
                        WHERE user_id = ? AND start_date = ? AND rental_id = ?
                        """)
                        .addPositionalValue(isReturned)
                        .addPositionalValue(endDate)
                        .addPositionalValue(rental.userId)
                        .addPositionalValue(rental.startDate)
                        .addPositionalValue(rental.rentalId)
                        .build()
        );

        cassandraSession.execute(
                SimpleStatement.builder("""
                        UPDATE rentals_by_shop
                        SET is_returned = ?, end_date = ?
                        WHERE shop_id = ? AND start_date = ? AND rental_id = ?
                        """)
                        .addPositionalValue(isReturned)
                        .addPositionalValue(endDate)
                        .addPositionalValue(rental.shopId)
                        .addPositionalValue(rental.startDate)
                        .addPositionalValue(rental.rentalId)
                        .build()
        );
    }

    private boolean shopExists(UUID shopId) {
        return cassandraSession.execute(
                        SimpleStatement.builder("SELECT shop_id FROM bookshops WHERE shop_id = ?")
                                .addPositionalValue(shopId)
                                .build())
                .one() != null;
    }

    private boolean isWithinRange(LocalDate value, LocalDate fromDate, LocalDate toDate) {
        return value != null && !value.isBefore(fromDate) && !value.isAfter(toDate);
    }

    private long uuidToPositiveLong(UUID value) {
        if (value == null) {
            return 0L;
        }

        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }

    private record UserDetails(String name, String surname, String phoneNumber, String email) {
    }

    private record UserStatusSnapshot(UUID userId, String previousStatus, String login) {
    }

        private record RentalSnapshot(
            UUID userId,
            UUID shopId,
            LocalDate startDate,
            UUID rentalId,
            Boolean previousReturned,
            LocalDate previousEndDate
        ) {
        }

    private record EmployeeDetails(String name, String surname) {
    }

    private record BookDetails(String title, String author) {
    }
}