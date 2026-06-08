package bench.app.service.cql.cassandra;

import bench.app.benchmark.RequestTimingContextHolder;
import bench.app.model.common.UserInactiveSegmentDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraInactiveUserSegmentDeleteService {
    private static final String SELECT_ALL_INACTIVE_USER_IDS = """
            SELECT user_id
            FROM users
            WHERE status = 'INACTIVE'
            ALLOW FILTERING
            """;

    private static final String SELECT_ALL_INACTIVE_USERS = """
            SELECT user_id, name, surname, phone_number, email, main_book_shop_id, status, login, permissions
            FROM users
            WHERE status = 'INACTIVE'
            ALLOW FILTERING
            """;

    private static final String SELECT_INACTIVE_USER_BY_ID = """
            SELECT user_id, name, surname, phone_number, email, main_book_shop_id, status, login, permissions
            FROM users
            WHERE user_id = ?
            """;

    private static final String SELECT_RECENT_USER_RESERVATION = """
            SELECT when_reserved
            FROM reservations_by_user
            WHERE user_id = ? AND when_reserved >= ?
            LIMIT 1
            """;

    private static final String SELECT_RECENT_USER_RENTAL_BY_START = """
            SELECT start_date
            FROM rentals_by_user
            WHERE user_id = ? AND start_date >= ?
            LIMIT 1
            """;

    private static final String SELECT_USER_CREDENTIALS = """
            SELECT password_hash, status
            FROM user_credentials_by_login
            WHERE login = ?
            """;

    private static final String SELECT_USER_RESERVATIONS = """
            SELECT user_id, when_reserved, reservation_id, book_id, book_title
            FROM reservations_by_user
            WHERE user_id = ?
            """;

    private static final String SELECT_USER_RENTALS = """
            SELECT user_id, start_date, rental_id, book_id, book_title, shop_id,
                   employee_id, is_returned, end_date, rental_method
            FROM rentals_by_user
            WHERE user_id = ?
            """;

    private static final String DELETE_RESERVATION = """
            DELETE FROM reservations_by_user
            WHERE user_id = ? AND when_reserved = ? AND reservation_id = ?
            """;

    private static final String DELETE_RENTAL_BY_USER = """
            DELETE FROM rentals_by_user
            WHERE user_id = ? AND start_date = ? AND rental_id = ?
            """;

    private static final String DELETE_RENTAL_BY_SHOP = """
            DELETE FROM rentals_by_shop
            WHERE shop_id = ? AND start_date = ? AND rental_id = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM users
            WHERE user_id = ?
            """;

    private static final String DELETE_CREDENTIALS = """
            DELETE FROM user_credentials_by_login
            WHERE login = ?
            """;

    private static final String INSERT_USER = """
            INSERT INTO users (user_id, name, surname, phone_number, email, main_book_shop_id, status, login, permissions)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_CREDENTIALS = """
            INSERT INTO user_credentials_by_login (login, user_id, password_hash, status)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO reservations_by_user (user_id, when_reserved, reservation_id, book_id, book_title)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL_BY_USER = """
            INSERT INTO rentals_by_user (
                user_id, start_date, rental_id, book_id, book_title, shop_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL_BY_SHOP = """
            INSERT INTO rentals_by_shop (
                shop_id, start_date, rental_id, book_id, user_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final CqlSession cassandraSession;
        private final RequestTimingContextHolder timingContextHolder;
    private final Map<String, List<UserSnapshot>> snapshots = new ConcurrentHashMap<>();

        @Autowired
        public CassandraInactiveUserSegmentDeleteService(
                        @Qualifier("cassandraSession") CqlSession cassandraSession,
                        RequestTimingContextHolder timingContextHolder
        ) {
        this.cassandraSession = cassandraSession;
                this.timingContextHolder = timingContextHolder;
    }

    public UserInactiveSegmentDeleteResult deleteInactiveUsersWithoutRecentActivity(int monthsThreshold, int segmentSize, boolean restoreAfterDelete) {
        if (monthsThreshold <= 0) {
            throw new IllegalArgumentException("monthsThreshold musi być większe od 0");
        }
        if (segmentSize <= 0) {
            throw new IllegalArgumentException("segmentSize musi być większe od 0");
        }

        if (restoreAfterDelete) {
            return restoreFromSnapshot(monthsThreshold, segmentSize);
        }

        LocalDate cutoff = LocalDate.now().minusMonths(monthsThreshold);
                List<UUID> candidateUserIds = new ArrayList<>();

                ResultSet users = cassandraSession.execute(SELECT_ALL_INACTIVE_USER_IDS);
        for (Row userRow : users) {
            UUID userId = userRow.getUuid("user_id");
                        if (userId == null || hasRecentActivity(userId, cutoff)) {
                continue;
            }
                        candidateUserIds.add(userId);
        }

                candidateUserIds.sort(Comparator.naturalOrder());
                List<UUID> candidateUserIdsLimitedMutable = candidateUserIds;
                if (candidateUserIdsLimitedMutable.size() > segmentSize) {
                        candidateUserIdsLimitedMutable = new ArrayList<>(candidateUserIdsLimitedMutable.subList(0, segmentSize));
        }
                final List<UUID> candidateUserIdsLimited = candidateUserIdsLimitedMutable;

                List<UserSnapshot> candidates = runOutsideTiming(() -> loadSnapshotsByUserIds(candidateUserIdsLimited));

        int deletedUsers = 0;
        int deletedAccounts = 0;
        int deletedReservations = 0;
        int deletedRentals = 0;

        for (UserSnapshot user : candidates) {
            for (ReservationSnapshot reservation : user.reservations()) {
                deletedReservations += cassandraSession.execute(
                        SimpleStatement.builder(DELETE_RESERVATION)
                                .addPositionalValues(reservation.userId(), reservation.whenReserved(), reservation.reservationId())
                                .build()
                ).wasApplied() ? 1 : 0;
            }

            for (RentalSnapshot rental : user.rentals()) {
                deletedRentals += cassandraSession.execute(
                        SimpleStatement.builder(DELETE_RENTAL_BY_USER)
                                .addPositionalValues(rental.userId(), rental.startDate(), rental.rentalId())
                                .build()
                ).wasApplied() ? 1 : 0;
                deletedRentals += cassandraSession.execute(
                        SimpleStatement.builder(DELETE_RENTAL_BY_SHOP)
                                .addPositionalValues(rental.shopId(), rental.startDate(), rental.rentalId())
                                .build()
                ).wasApplied() ? 1 : 0;
            }

            if (user.login() != null && !user.login().isBlank()) {
                deletedAccounts += cassandraSession.execute(
                        SimpleStatement.builder(DELETE_CREDENTIALS)
                                .addPositionalValue(user.login())
                                .build()
                ).wasApplied() ? 1 : 0;
            }

            deletedUsers += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_USER)
                            .addPositionalValue(user.userId())
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        runOutsideTiming(() -> snapshots.put(snapshotKey(monthsThreshold, segmentSize), List.copyOf(candidates)));

        return new UserInactiveSegmentDeleteResult(
                monthsThreshold,
                segmentSize,
                candidates.size(),
                deletedUsers,
                0,
                deletedAccounts,
                deletedReservations,
                deletedRentals,
                0,
                0,
                0,
                0,
                0,
                false
        );
    }

    private UserInactiveSegmentDeleteResult restoreFromSnapshot(int monthsThreshold, int segmentSize) {
        String key = snapshotKey(monthsThreshold, segmentSize);
        List<UserSnapshot> users = snapshots.get(key);
        if (users == null) {
            throw new IllegalStateException("Brak snapshotu D4 dla monthsThreshold=" + monthsThreshold + " segmentSize=" + segmentSize);
        }

        int restoredUsers = 0;
        int restoredAccounts = 0;
        int restoredReservations = 0;
        int restoredRentals = 0;

        for (UserSnapshot user : users) {
            restoredUsers += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_USER)
                            .addPositionalValues(
                                    user.userId(),
                                    user.name(),
                                    user.surname(),
                                    user.phoneNumber(),
                                    user.email(),
                                    user.mainBookShopId(),
                                    user.status(),
                                    user.login(),
                                    user.permissions()
                            )
                            .build()
            ).wasApplied() ? 1 : 0;

            if (user.login() != null && !user.login().isBlank() && user.passwordHash() != null) {
                restoredAccounts += cassandraSession.execute(
                        SimpleStatement.builder(INSERT_CREDENTIALS)
                                .addPositionalValues(
                                        user.login(),
                                        user.userId(),
                                        user.passwordHash(),
                                        user.credentialsStatus() == null ? user.status() : user.credentialsStatus()
                                )
                                .build()
                ).wasApplied() ? 1 : 0;
            }

            for (ReservationSnapshot reservation : user.reservations()) {
                restoredReservations += cassandraSession.execute(
                        SimpleStatement.builder(INSERT_RESERVATION)
                                .addPositionalValues(
                                        reservation.userId(),
                                        reservation.whenReserved(),
                                        reservation.reservationId(),
                                        reservation.bookId(),
                                        reservation.bookTitle()
                                )
                                .build()
                ).wasApplied() ? 1 : 0;
            }

            for (RentalSnapshot rental : user.rentals()) {
                restoredRentals += cassandraSession.execute(
                        SimpleStatement.builder(INSERT_RENTAL_BY_USER)
                                .addPositionalValues(
                                        rental.userId(),
                                        rental.startDate(),
                                        rental.rentalId(),
                                        rental.bookId(),
                                        rental.bookTitle(),
                                        rental.shopId(),
                                        rental.employeeId(),
                                        rental.isReturned(),
                                        rental.endDate(),
                                        rental.rentalMethod()
                                )
                                .build()
                ).wasApplied() ? 1 : 0;

                restoredRentals += cassandraSession.execute(
                        SimpleStatement.builder(INSERT_RENTAL_BY_SHOP)
                                .addPositionalValues(
                                        rental.shopId(),
                                        rental.startDate(),
                                        rental.rentalId(),
                                        rental.bookId(),
                                        rental.userId(),
                                        rental.employeeId(),
                                        rental.isReturned(),
                                        rental.endDate(),
                                        rental.rentalMethod()
                                )
                                .build()
                ).wasApplied() ? 1 : 0;
            }
        }

        snapshots.remove(key);

        return new UserInactiveSegmentDeleteResult(
                monthsThreshold,
                segmentSize,
                users.size(),
                0,
                0,
                0,
                0,
                0,
                restoredUsers,
                0,
                restoredAccounts,
                restoredReservations,
                restoredRentals,
                true
        );
    }

        private <T> T runOutsideTiming(java.util.function.Supplier<T> supplier) {
                if (timingContextHolder == null) {
                        return supplier.get();
                }
                return timingContextHolder.excludeFromTiming(supplier);
        }

        private List<UserSnapshot> loadSnapshotsByUserIds(List<UUID> userIds) {
                List<UserSnapshot> snapshotsList = new ArrayList<>(userIds.size());
                for (UUID userId : userIds) {
                        Row userRow = cassandraSession.execute(
                                        SimpleStatement.builder(SELECT_INACTIVE_USER_BY_ID)
                                                        .addPositionalValue(userId)
                                                        .build()
                        ).one();
                        if (userRow == null) {
                                continue;
                        }
                        snapshotsList.add(readUserSnapshot(userRow));
                }
                return snapshotsList;
        }

        private UserSnapshot readUserSnapshot(Row userRow) {
        UUID userId = userRow.getUuid("user_id");
        String login = userRow.getString("login");
        String passwordHash = null;
        String credentialsStatus = null;
        if (login != null && !login.isBlank()) {
            Row credentials = cassandraSession.execute(
                    SimpleStatement.builder(SELECT_USER_CREDENTIALS)
                            .addPositionalValue(login)
                            .build()
            ).one();
            if (credentials != null) {
                passwordHash = credentials.getString("password_hash");
                credentialsStatus = credentials.getString("status");
            }
        }

        List<ReservationSnapshot> reservations = new ArrayList<>();
        ResultSet reservationRows = cassandraSession.execute(
                SimpleStatement.builder(SELECT_USER_RESERVATIONS)
                        .addPositionalValue(userId)
                        .build()
        );
        for (Row row : reservationRows) {
            reservations.add(new ReservationSnapshot(
                    row.getUuid("user_id"),
                                        row.getLocalDate("when_reserved"),
                    row.getUuid("reservation_id"),
                    row.getUuid("book_id"),
                    row.getString("book_title")
            ));
        }

        List<RentalSnapshot> rentals = new ArrayList<>();
        ResultSet rentalRows = cassandraSession.execute(
                SimpleStatement.builder(SELECT_USER_RENTALS)
                        .addPositionalValue(userId)
                        .build()
        );
        for (Row row : rentalRows) {
            rentals.add(new RentalSnapshot(
                    row.getUuid("user_id"),
                    row.getLocalDate("start_date"),
                    row.getUuid("rental_id"),
                    row.getUuid("book_id"),
                    row.getString("book_title"),
                    row.getUuid("shop_id"),
                    row.getUuid("employee_id"),
                    Boolean.TRUE.equals(row.getBoolean("is_returned")),
                    row.getLocalDate("end_date"),
                    row.getString("rental_method") == null ? "STANDARD" : row.getString("rental_method")
            ));
        }

        return new UserSnapshot(
                userId,
                userRow.getString("name"),
                userRow.getString("surname"),
                userRow.getString("phone_number"),
                userRow.getString("email"),
                userRow.getUuid("main_book_shop_id"),
                userRow.getString("status"),
                login,
                userRow.getSet("permissions", String.class),
                passwordHash,
                credentialsStatus,
                reservations,
                rentals
        );
    }

        private boolean hasRecentActivity(UUID userId, LocalDate cutoffDate) {
                Row recentReservation = cassandraSession.execute(
                                SimpleStatement.builder(SELECT_RECENT_USER_RESERVATION)
                                                .addPositionalValues(userId, cutoffDate)
                                                .build()
                ).one();
                if (recentReservation != null) {
                        return true;
                }

                Row recentRentalStart = cassandraSession.execute(
                                SimpleStatement.builder(SELECT_RECENT_USER_RENTAL_BY_START)
                                                .addPositionalValues(userId, cutoffDate)
                                                .build()
                ).one();
                if (recentRentalStart != null) {
                        return true;
                }

                ResultSet rentals = cassandraSession.execute(
                                SimpleStatement.builder(SELECT_USER_RENTALS)
                                                .addPositionalValue(userId)
                                                .build()
                );
                for (Row row : rentals) {
                        LocalDate endDate = row.getLocalDate("end_date");
                        if (endDate != null && !endDate.isBefore(cutoffDate)) {
                                return true;
                        }
                }

                return false;
        }

    private String snapshotKey(int monthsThreshold, int segmentSize) {
        return monthsThreshold + ":" + segmentSize;
    }

    private record UserSnapshot(
            UUID userId,
            String name,
            String surname,
            String phoneNumber,
            String email,
            UUID mainBookShopId,
            String status,
            String login,
            Set<String> permissions,
            String passwordHash,
            String credentialsStatus,
            List<ReservationSnapshot> reservations,
            List<RentalSnapshot> rentals
    ) {
    }

    private record ReservationSnapshot(
            UUID userId,
            LocalDate whenReserved,
            UUID reservationId,
            UUID bookId,
            String bookTitle
    ) {
    }

    private record RentalSnapshot(
            UUID userId,
            LocalDate startDate,
            UUID rentalId,
            UUID bookId,
            String bookTitle,
            UUID shopId,
            UUID employeeId,
            boolean isReturned,
            LocalDate endDate,
            String rentalMethod
    ) {
    }

}
