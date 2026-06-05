package bench.app.service.sql;

import bench.app.benchmark.BookReservationSnapshot;
import bench.app.benchmark.BookReservationSnapshotStore;
import bench.app.benchmark.RequestTimingContextHolder;
import bench.app.model.common.BookReservationCreateResult;
import bench.app.model.common.BookReservationDeleteResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;

@Service
public class PostgresBookReservationCreateService {
    private static final String DB_ENGINE = "POSTGRESQL";

    private static final String SELECT_LAST_RESERVATION_ID = """
            SELECT id
            FROM bench.bookreservation
            ORDER BY id DESC
            LIMIT 1
            """;

    private static final String INSERT_RESERVATION_IF_EXISTS = """
            INSERT INTO bench.bookreservation (id, bookid, userid, whenreserved)
            SELECT ?, ?, ?, ?
            WHERE EXISTS (SELECT 1 FROM bench.book WHERE id = ?)
              AND EXISTS (SELECT 1 FROM bench.bookshopuser WHERE id = ?)
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO bench.bookreservation (id, bookid, userid, whenreserved)
            VALUES (?, ?, ?, ?)
            """;

    private static final String DELETE_RESERVATION = """
            DELETE FROM bench.bookreservation
            WHERE id = ?
            """;

    private static final String SELECT_RESERVATION_BY_ID = """
            SELECT bookid AS book_id, userid AS user_id, whenreserved AS when_reserved
            FROM bench.bookreservation
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BookReservationSnapshotStore snapshotStore;
        private final RequestTimingContextHolder timingContextHolder;

    public PostgresBookReservationCreateService(
            @Qualifier("postgresDataSource") DataSource dataSource,
                        BookReservationSnapshotStore snapshotStore,
                        RequestTimingContextHolder timingContextHolder
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
                this.timingContextHolder = timingContextHolder;
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public BookReservationCreateResult createReservation(long bookId, long userId, LocalDate whenReserved, boolean restoreAfterCreate) {
        long reservationId = findNextReservationId();

        LocalDate effectiveDate = whenReserved == null ? LocalDate.now() : whenReserved;
        int insertedRows = jdbcTemplate.update(
                                INSERT_RESERVATION_IF_EXISTS,
                reservationId,
                bookId,
                userId,
                                Date.valueOf(effectiveDate),
                                bookId,
                                userId
        );
                if (insertedRows == 0) {
                        throw new IllegalArgumentException("Nie można utworzyć rezerwacji: nie istnieje bookId albo userId");
                }

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows = jdbcTemplate.update(DELETE_RESERVATION, reservationId);
            existsAfterOperation = false;
        }

        return new BookReservationCreateResult(
                reservationId,
                bookId,
                userId,
                effectiveDate,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public BookReservationDeleteResult deleteReservation(long reservationId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreReservationFromSnapshot(reservationId);
        }

        BookReservationSnapshot snapshot = timingContextHolder.excludeFromTiming(() -> readReservationSnapshot(reservationId));

        int deletedRows = jdbcTemplate.update(DELETE_RESERVATION, reservationId);
        timingContextHolder.excludeFromTiming(() -> snapshotStore.save(DB_ENGINE, reservationId, snapshot));

        return new BookReservationDeleteResult(
                reservationId,
                snapshot.bookId(),
                snapshot.userId(),
                snapshot.whenReserved(),
                false,
                false,
                deletedRows,
                0
        );
    }

    private BookReservationDeleteResult restoreReservationFromSnapshot(long reservationId) {
        BookReservationSnapshot snapshot = snapshotStore.find(DB_ENGINE, reservationId)
                .orElseThrow(() -> new IllegalStateException("Brak snapshotu rezerwacji o id=" + reservationId));

        int insertedRows = jdbcTemplate.update(
                INSERT_RESERVATION,
                reservationId,
                snapshot.bookId(),
                snapshot.userId(),
                Date.valueOf(snapshot.whenReserved())
        );

        snapshotStore.removeIfMatches(DB_ENGINE, reservationId, snapshot);

        return new BookReservationDeleteResult(
                reservationId,
                snapshot.bookId(),
                snapshot.userId(),
                snapshot.whenReserved(),
                true,
                true,
                0,
                insertedRows
        );
    }

    private BookReservationSnapshot readReservationSnapshot(long reservationId) {
        var reservationRow = jdbcTemplate.query(
                SELECT_RESERVATION_BY_ID,
                rs -> rs.next() ? new Object[]{rs.getLong("book_id"), rs.getLong("user_id"), rs.getDate("when_reserved").toLocalDate()} : null,
                reservationId
        );
        if (reservationRow == null) {
            throw new IllegalArgumentException("Nie znaleziono rezerwacji o id=" + reservationId);
        }

        long bookId = (Long) reservationRow[0];
        long userId = (Long) reservationRow[1];
        LocalDate whenReserved = (LocalDate) reservationRow[2];
        return new BookReservationSnapshot(bookId, userId, whenReserved);
    }

        private long findNextReservationId() {
                Long lastId = jdbcTemplate.query(
                                SELECT_LAST_RESERVATION_ID,
                                rs -> rs.next() ? rs.getLong("id") : null
                );
                return lastId == null ? 1L : lastId + 1L;
        }
}