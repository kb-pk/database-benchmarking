package bench.app.service.sql;

import bench.app.benchmark.BookReservationSnapshot;
import bench.app.benchmark.BookReservationSnapshotStore;
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
public class MssqlBookReservationCreateService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_NEXT_RESERVATION_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.BookReservation
            """;

    private static final String CHECK_BOOK_EXISTS = """
            SELECT COUNT(*)
            FROM bench.Book
            WHERE id = ?
            """;

    private static final String CHECK_USER_EXISTS = """
            SELECT COUNT(*)
            FROM bench.BookShopUser
            WHERE id = ?
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO bench.BookReservation (id, bookId, userId, whenReserved)
            VALUES (?, ?, ?, ?)
            """;

    private static final String DELETE_RESERVATION = """
            DELETE FROM bench.BookReservation
            WHERE id = ?
            """;

    private static final String SELECT_RESERVATION_BY_ID = """
            SELECT bookId AS book_id, userId AS user_id, whenReserved AS when_reserved
            FROM bench.BookReservation
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BookReservationSnapshotStore snapshotStore;

    public MssqlBookReservationCreateService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            BookReservationSnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookReservationCreateResult createReservation(long bookId, long userId, LocalDate whenReserved, boolean restoreAfterCreate) {
        if (!exists(CHECK_BOOK_EXISTS, bookId)) {
            throw new IllegalArgumentException("Książka o podanym bookId nie istnieje");
        }
        if (!exists(CHECK_USER_EXISTS, userId)) {
            throw new IllegalArgumentException("Użytkownik o podanym userId nie istnieje");
        }

        Long reservationId = jdbcTemplate.query(
                SELECT_NEXT_RESERVATION_ID,
                rs -> rs.next() ? rs.getLong("next_id") : null
        );
        if (reservationId == null) {
            throw new IllegalArgumentException("Nie udało się ustalić nowego id rezerwacji");
        }

        LocalDate effectiveDate = whenReserved == null ? LocalDate.now() : whenReserved;
        int insertedRows = jdbcTemplate.update(
                INSERT_RESERVATION,
                reservationId,
                bookId,
                userId,
                Date.valueOf(effectiveDate)
        );

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

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookReservationDeleteResult deleteReservation(long reservationId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreReservationFromSnapshot(reservationId);
        }

        BookReservationSnapshot snapshot = readReservationSnapshot(reservationId);

        int deletedRows = jdbcTemplate.update(DELETE_RESERVATION, reservationId);
        snapshotStore.save(DB_ENGINE, reservationId, snapshot);

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

    private boolean exists(String sql, long id) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id);
        return count != null && count > 0;
    }
}