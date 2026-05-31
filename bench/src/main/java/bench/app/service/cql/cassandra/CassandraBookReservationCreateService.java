package bench.app.service.cql.cassandra;

import bench.app.model.common.BookReservationCreateResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CassandraBookReservationCreateService {
    private static final String INSERT_RESERVATION = """
            INSERT INTO reservations_by_user (user_id, when_reserved, reservation_id, book_id, book_title)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String DELETE_RESERVATION = """
            DELETE FROM reservations_by_user
            WHERE user_id = ? AND when_reserved = ? AND reservation_id = ?
            """;

    private final CqlSession cassandraSession;

    public CassandraBookReservationCreateService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookReservationCreateResult createReservation(UUID bookId, UUID userId, LocalDate whenReserved, boolean restoreAfterCreate) {
        if (!userExists(userId)) {
            throw new IllegalArgumentException("Użytkownik o podanym userId nie istnieje");
        }

        Row bookRow = cassandraSession.execute(
                SimpleStatement.builder("SELECT title FROM books_by_shop WHERE book_id = ? LIMIT 1 ALLOW FILTERING")
                                .addPositionalValue(bookId)
                                .build())
                .one();
        if (bookRow == null) {
            throw new IllegalArgumentException("Książka o podanym bookId nie istnieje");
        }

        String bookTitle = bookRow.getString("title");
        UUID reservationId = UUID.randomUUID();
        LocalDate effectiveWhenReserved = whenReserved == null ? LocalDate.now() : whenReserved;

        int insertedRows = cassandraSession.execute(
                SimpleStatement.builder(INSERT_RESERVATION)
                        .addPositionalValues(userId, effectiveWhenReserved, reservationId, bookId, bookTitle)
                        .build()
        ).wasApplied() ? 1 : 0;

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows = cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RESERVATION)
                            .addPositionalValues(userId, effectiveWhenReserved, reservationId)
                            .build()
            ).wasApplied() ? 1 : 0;
            existsAfterOperation = false;
        }

        return new BookReservationCreateResult(
                uuidToPositiveLong(reservationId),
                uuidToPositiveLong(bookId),
                uuidToPositiveLong(userId),
                effectiveWhenReserved,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

    private boolean userExists(UUID userId) {
        return cassandraSession.execute(
                        SimpleStatement.builder("SELECT user_id FROM users WHERE user_id = ?")
                                .addPositionalValue(userId)
                                .build())
                .one() != null;
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }
}