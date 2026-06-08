package bench.app.service.cql.cassandra;

import bench.app.benchmark.RequestTimingContextHolder;
import bench.app.model.common.BookReservationBulkDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraBookReservationBulkDeleteService {
    private static final String SELECT_ALL_RESERVATIONS = """
            SELECT user_id, when_reserved, reservation_id, book_id, book_title
            FROM reservations_by_user
            """;

            private static final String SELECT_ALL_RENTALS = """
                SELECT user_id, start_date, book_id
                FROM rentals_by_user
                """;

            private static final String SELECT_RESERVATION_BY_KEY = """
                SELECT user_id, when_reserved, reservation_id, book_id, book_title
                FROM reservations_by_user
                WHERE user_id = ? AND when_reserved = ? AND reservation_id = ?
                """;

    private static final String DELETE_RESERVATION = """
            DELETE FROM reservations_by_user
            WHERE user_id = ? AND when_reserved = ? AND reservation_id = ?
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO reservations_by_user (user_id, when_reserved, reservation_id, book_id, book_title)
            VALUES (?, ?, ?, ?, ?)
            """;

    private final CqlSession cassandraSession;
    private final RequestTimingContextHolder timingContextHolder;
    private final Map<Integer, List<ReservationSnapshot>> snapshotsByThreshold = new ConcurrentHashMap<>();

    @Autowired
    public CassandraBookReservationBulkDeleteService(
            @Qualifier("cassandraSession") CqlSession cassandraSession,
            RequestTimingContextHolder timingContextHolder
    ) {
        this.cassandraSession = cassandraSession;
        this.timingContextHolder = timingContextHolder;
    }

    public BookReservationBulkDeleteResult deleteOldUnfinalizedReservations(int monthsThreshold, boolean restoreAfterDelete) {
        if (monthsThreshold <= 0) {
            throw new IllegalArgumentException("monthsThreshold musi być większe od 0");
        }

        if (restoreAfterDelete) {
            return restoreFromSnapshot(monthsThreshold);
        }

        LocalDate cutoffDate = LocalDate.now().minusMonths(monthsThreshold);
        List<ReservationDeleteKey> matchedKeys = new ArrayList<>();
        Map<UserBookKey, LocalDate> latestRentalDateByUserBook = loadLatestRentalDatesByUserBook();

        ResultSet reservations = cassandraSession.execute(SELECT_ALL_RESERVATIONS);
        for (Row row : reservations) {
            LocalDate whenReserved = row.getLocalDate("when_reserved");
            if (whenReserved == null || !whenReserved.isBefore(cutoffDate)) {
                continue;
            }

            UUID userId = row.getUuid("user_id");
            UUID bookId = row.getUuid("book_id");
            if (hasRentalAfterReservation(latestRentalDateByUserBook, userId, bookId, whenReserved)) {
                continue;
            }

            matchedKeys.add(new ReservationDeleteKey(
                    userId,
                    whenReserved,
                    row.getUuid("reservation_id")
            ));
        }

        List<ReservationSnapshot> snapshotRows = runOutsideTiming(() -> loadSnapshotsByKeys(matchedKeys));

        int deletedRows = 0;
        for (ReservationDeleteKey key : matchedKeys) {
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RESERVATION)
                            .addPositionalValues(key.userId(), key.whenReserved(), key.reservationId())
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        runOutsideTiming(() -> snapshotsByThreshold.put(monthsThreshold, List.copyOf(snapshotRows)));

        return new BookReservationBulkDeleteResult(
                monthsThreshold,
                matchedKeys.size(),
                deletedRows,
                0,
                false
        );
    }

    private <T> T runOutsideTiming(java.util.function.Supplier<T> supplier) {
        if (timingContextHolder == null) {
            return supplier.get();
        }
        return timingContextHolder.excludeFromTiming(supplier);
    }

    private List<ReservationSnapshot> loadSnapshotsByKeys(List<ReservationDeleteKey> keys) {
        List<ReservationSnapshot> snapshots = new ArrayList<>(keys.size());
        for (ReservationDeleteKey key : keys) {
            Row row = cassandraSession.execute(
                    SimpleStatement.builder(SELECT_RESERVATION_BY_KEY)
                            .addPositionalValues(key.userId(), key.whenReserved(), key.reservationId())
                            .build()
            ).one();
            if (row == null) {
                continue;
            }
            snapshots.add(new ReservationSnapshot(
                    row.getUuid("user_id"),
                    row.getLocalDate("when_reserved"),
                    row.getUuid("reservation_id"),
                    row.getUuid("book_id"),
                    row.getString("book_title")
            ));
        }
        return snapshots;
    }

    private BookReservationBulkDeleteResult restoreFromSnapshot(int monthsThreshold) {
        List<ReservationSnapshot> snapshots = snapshotsByThreshold.get(monthsThreshold);
        if (snapshots == null) {
            throw new IllegalStateException("Brak snapshotu D3 dla monthsThreshold=" + monthsThreshold);
        }

        int restoredRows = 0;
        for (ReservationSnapshot snapshot : snapshots) {
            restoredRows += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_RESERVATION)
                            .addPositionalValues(
                                    snapshot.userId(),
                                    snapshot.whenReserved(),
                                    snapshot.reservationId(),
                                    snapshot.bookId(),
                                    snapshot.bookTitle()
                            )
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshotsByThreshold.remove(monthsThreshold);

        return new BookReservationBulkDeleteResult(
                monthsThreshold,
                snapshots.size(),
                0,
                restoredRows,
                true
        );
    }

    private Map<UserBookKey, LocalDate> loadLatestRentalDatesByUserBook() {
        Map<UserBookKey, LocalDate> latestRentalDateByUserBook = new HashMap<>();
        ResultSet rentals = cassandraSession.execute(SELECT_ALL_RENTALS);

        for (Row rental : rentals) {
            UUID userId = rental.getUuid("user_id");
            UUID bookId = rental.getUuid("book_id");
            LocalDate startDate = rental.getLocalDate("start_date");
            if (userId == null || bookId == null || startDate == null) {
                continue;
            }

            UserBookKey key = new UserBookKey(userId, bookId);
            LocalDate currentLatest = latestRentalDateByUserBook.get(key);
            if (currentLatest == null || startDate.isAfter(currentLatest)) {
                latestRentalDateByUserBook.put(key, startDate);
            }
        }

        return latestRentalDateByUserBook;
    }

    private boolean hasRentalAfterReservation(
            Map<UserBookKey, LocalDate> latestRentalDateByUserBook,
            UUID userId,
            UUID bookId,
            LocalDate whenReserved
    ) {
        LocalDate latestRentalDate = latestRentalDateByUserBook.get(new UserBookKey(userId, bookId));
        return latestRentalDate != null && !latestRentalDate.isBefore(whenReserved);
    }

    private record UserBookKey(UUID userId, UUID bookId) {
    }

    private record ReservationSnapshot(
            UUID userId,
            LocalDate whenReserved,
            UUID reservationId,
            UUID bookId,
            String bookTitle
    ) {
    }

        private record ReservationDeleteKey(
            UUID userId,
            LocalDate whenReserved,
            UUID reservationId
        ) {
        }
}
