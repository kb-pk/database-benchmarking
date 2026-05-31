package bench.app.service.cql.cassandra;

import bench.app.model.common.BookReservationDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraBookReservationDeleteService {
    private static final String SELECT_RESERVATION_BY_ID = """
            SELECT user_id, when_reserved, reservation_id, book_id, book_title
            FROM reservations_by_user
            WHERE reservation_id = ?
            LIMIT 1 ALLOW FILTERING
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
    private final Map<UUID, ReservationSnapshot> snapshots = new ConcurrentHashMap<>();

    public CassandraBookReservationDeleteService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookReservationDeleteResult deleteReservation(UUID reservationId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(reservationId);
        }

        ReservationSnapshot snapshot = findReservationSnapshot(reservationId);
        int deletedRows = cassandraSession.execute(
                SimpleStatement.builder(DELETE_RESERVATION)
                        .addPositionalValues(snapshot.userId(), snapshot.whenReserved(), snapshot.reservationId())
                        .build()
        ).wasApplied() ? 1 : 0;

        snapshots.put(reservationId, snapshot);

        return new BookReservationDeleteResult(
                uuidToPositiveLong(snapshot.reservationId()),
                uuidToPositiveLong(snapshot.bookId()),
                uuidToPositiveLong(snapshot.userId()),
                snapshot.whenReserved(),
                false,
                false,
                deletedRows,
                0
        );
    }

    private BookReservationDeleteResult restoreFromSnapshot(UUID reservationId) {
        ReservationSnapshot snapshot = snapshots.get(reservationId);
        if (snapshot == null) {
            throw new IllegalStateException("Brak snapshotu rezerwacji o id=" + reservationId);
        }

        int insertedRows = cassandraSession.execute(
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

        snapshots.remove(reservationId);

        return new BookReservationDeleteResult(
                uuidToPositiveLong(snapshot.reservationId()),
                uuidToPositiveLong(snapshot.bookId()),
                uuidToPositiveLong(snapshot.userId()),
                snapshot.whenReserved(),
                true,
                true,
                0,
                insertedRows
        );
    }

    private ReservationSnapshot findReservationSnapshot(UUID reservationId) {
        Row row = cassandraSession.execute(
                SimpleStatement.builder(SELECT_RESERVATION_BY_ID)
                        .addPositionalValue(reservationId)
                        .build()
        ).one();

        if (row == null) {
            throw new IllegalArgumentException("Nie znaleziono rezerwacji o id=" + reservationId);
        }

        return new ReservationSnapshot(
                row.getUuid("user_id"),
                row.getLocalDate("when_reserved"),
                row.getUuid("reservation_id"),
                row.getUuid("book_id"),
                row.getString("book_title")
        );
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
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
