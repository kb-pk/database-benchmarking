package bench.app.service.cql.cassandra;

import bench.app.model.common.BookReservationBulkDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    private static final String SELECT_RENTALS_FOR_USER_FROM_DATE = """
            SELECT book_id
            FROM rentals_by_user
            WHERE user_id = ? AND start_date >= ?
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
    private final Map<Integer, List<ReservationSnapshot>> snapshotsByThreshold = new ConcurrentHashMap<>();

    public CassandraBookReservationBulkDeleteService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookReservationBulkDeleteResult deleteOldUnfinalizedReservations(int monthsThreshold, boolean restoreAfterDelete) {
        if (monthsThreshold <= 0) {
            throw new IllegalArgumentException("monthsThreshold musi być większe od 0");
        }

        if (restoreAfterDelete) {
            return restoreFromSnapshot(monthsThreshold);
        }

        LocalDate cutoffDate = LocalDate.now().minusMonths(monthsThreshold);
        List<ReservationSnapshot> matched = new ArrayList<>();

        ResultSet reservations = cassandraSession.execute(SELECT_ALL_RESERVATIONS);
        for (Row row : reservations) {
            LocalDate whenReserved = row.getLocalDate("when_reserved");
            if (whenReserved == null || !whenReserved.isBefore(cutoffDate)) {
                continue;
            }

            UUID userId = row.getUuid("user_id");
            UUID bookId = row.getUuid("book_id");
            if (hasRentalAfterReservation(userId, bookId, whenReserved)) {
                continue;
            }

            matched.add(new ReservationSnapshot(
                    userId,
                    whenReserved,
                    row.getUuid("reservation_id"),
                    bookId,
                    row.getString("book_title")
            ));
        }

        int deletedRows = 0;
        for (ReservationSnapshot snapshot : matched) {
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RESERVATION)
                            .addPositionalValues(snapshot.userId(), snapshot.whenReserved(), snapshot.reservationId())
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshotsByThreshold.put(monthsThreshold, List.copyOf(matched));

        return new BookReservationBulkDeleteResult(
                monthsThreshold,
                matched.size(),
                deletedRows,
                0,
                false
        );
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

    private boolean hasRentalAfterReservation(UUID userId, UUID bookId, LocalDate whenReserved) {
        ResultSet rentals = cassandraSession.execute(
                SimpleStatement.builder(SELECT_RENTALS_FOR_USER_FROM_DATE)
                        .addPositionalValues(userId, whenReserved)
                        .build()
        );

        for (Row rental : rentals) {
            UUID rentalBookId = rental.getUuid("book_id");
            if (bookId.equals(rentalBookId)) {
                return true;
            }
        }
        return false;
    }

    private record ReservationSnapshot(
            UUID userId,
            LocalDate whenReserved,
            UUID reservationId,
            UUID bookId,
            String bookTitle
    ) {
    }
}
