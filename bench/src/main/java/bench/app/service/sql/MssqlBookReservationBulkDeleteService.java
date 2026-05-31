package bench.app.service.sql;

import bench.app.benchmark.BookReservationBulkSnapshot;
import bench.app.benchmark.BookReservationBulkSnapshotStore;
import bench.app.model.common.BookReservationBulkDeleteResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.List;

@Service
public class MssqlBookReservationBulkDeleteService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_MATCHING_RESERVATIONS = """
            SELECT br.id AS reservation_id,
                   br.bookId AS book_id,
                   br.userId AS user_id,
                   br.whenReserved AS when_reserved
            FROM bench.BookReservation br
            WHERE br.whenReserved < DATEADD(MONTH, -?, CAST(GETDATE() AS date))
              AND NOT EXISTS (
                SELECT 1
                FROM bench.BookRental r
                WHERE r.bookId = br.bookId
                  AND r.userId = br.userId
                  AND r.startDate >= br.whenReserved
            )
            """;

    private static final String DELETE_MATCHING_RESERVATIONS = """
            DELETE br
            FROM bench.BookReservation br
            WHERE br.whenReserved < DATEADD(MONTH, -?, CAST(GETDATE() AS date))
              AND NOT EXISTS (
                SELECT 1
                FROM bench.BookRental r
                WHERE r.bookId = br.bookId
                  AND r.userId = br.userId
                  AND r.startDate >= br.whenReserved
            )
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO bench.BookReservation (id, bookId, userId, whenReserved)
            VALUES (?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BookReservationBulkSnapshotStore snapshotStore;

    public MssqlBookReservationBulkDeleteService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            BookReservationBulkSnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookReservationBulkDeleteResult deleteOldUnfinalizedReservations(int monthsThreshold, boolean restoreAfterDelete) {
        if (monthsThreshold <= 0) {
            throw new IllegalArgumentException("monthsThreshold musi być większe od 0");
        }

        if (restoreAfterDelete) {
            return restoreFromSnapshot(monthsThreshold);
        }

        List<BookReservationBulkSnapshot> matchedRows = jdbcTemplate.query(
                SELECT_MATCHING_RESERVATIONS,
                (rs, rowNum) -> new BookReservationBulkSnapshot(
                        rs.getLong("reservation_id"),
                        rs.getLong("book_id"),
                        rs.getLong("user_id"),
                        rs.getDate("when_reserved").toLocalDate()
                ),
                monthsThreshold
        );

        int deletedRows = jdbcTemplate.update(DELETE_MATCHING_RESERVATIONS, monthsThreshold);
        snapshotStore.save(DB_ENGINE, monthsThreshold, matchedRows);

        return new BookReservationBulkDeleteResult(
                monthsThreshold,
                matchedRows.size(),
                deletedRows,
                0,
                false
        );
    }

    private BookReservationBulkDeleteResult restoreFromSnapshot(int monthsThreshold) {
        List<BookReservationBulkSnapshot> snapshotRows = snapshotStore.find(DB_ENGINE, monthsThreshold)
                .orElseThrow(() -> new IllegalStateException("Brak snapshotu D3 dla monthsThreshold=" + monthsThreshold));

        int restoredRows = 0;
        for (BookReservationBulkSnapshot row : snapshotRows) {
            restoredRows += jdbcTemplate.update(
                    INSERT_RESERVATION,
                    row.reservationId(),
                    row.bookId(),
                    row.userId(),
                    Date.valueOf(row.whenReserved())
            );
        }

        snapshotStore.remove(DB_ENGINE, monthsThreshold);

        return new BookReservationBulkDeleteResult(
                monthsThreshold,
                snapshotRows.size(),
                0,
                restoredRows,
                true
        );
    }
}