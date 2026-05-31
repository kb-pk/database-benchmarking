package bench.app.service.sql;

import bench.app.benchmark.BookRentalSnapshot;
import bench.app.benchmark.BookRentalSnapshotStore;
import bench.app.model.common.BookRentalDeleteResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;

@Service
public class MssqlBookRentalDeleteService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_RENTAL_BY_ID = """
            SELECT bookId AS book_id,
                   userId AS user_id,
                   employeeId AS employee_id,
                   bookShopId AS book_shop_id,
                   rentalMethodId AS rental_method_id,
                   isReturned AS is_returned,
                   startDate AS start_date,
                   endDate AS end_date
            FROM bench.BookRental
            WHERE id = ?
            """;

    private static final String DELETE_RENTAL = """
            DELETE FROM bench.BookRental
            WHERE id = ?
            """;

    private static final String INSERT_RENTAL = """
            INSERT INTO bench.BookRental (id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BookRentalSnapshotStore snapshotStore;

    public MssqlBookRentalDeleteService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            BookRentalSnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookRentalDeleteResult deleteRental(long rentalId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(rentalId);
        }

        BookRentalSnapshot snapshot = readSnapshot(rentalId);
        int deletedRows = jdbcTemplate.update(DELETE_RENTAL, rentalId);
        snapshotStore.save(DB_ENGINE, rentalId, snapshot);

        return new BookRentalDeleteResult(
                rentalId,
                snapshot.bookId(),
                snapshot.userId(),
                snapshot.employeeId(),
                snapshot.bookShopId(),
                snapshot.rentalMethodId(),
                snapshot.isReturned(),
                snapshot.startDate(),
                snapshot.endDate(),
                false,
                false,
                deletedRows,
                0
        );
    }

    private BookRentalDeleteResult restoreFromSnapshot(long rentalId) {
        BookRentalSnapshot snapshot = snapshotStore.find(DB_ENGINE, rentalId)
                .orElseThrow(() -> new IllegalStateException("Brak snapshotu wypożyczenia o id=" + rentalId));

        int insertedRows = jdbcTemplate.update(
                INSERT_RENTAL,
                rentalId,
                snapshot.bookId(),
                snapshot.userId(),
                snapshot.employeeId(),
                snapshot.bookShopId(),
                snapshot.isReturned() ? 1 : 0,
                Date.valueOf(snapshot.startDate()),
                snapshot.endDate() == null ? null : Date.valueOf(snapshot.endDate()),
                snapshot.rentalMethodId()
        );

        snapshotStore.removeIfMatches(DB_ENGINE, rentalId, snapshot);

        return new BookRentalDeleteResult(
                rentalId,
                snapshot.bookId(),
                snapshot.userId(),
                snapshot.employeeId(),
                snapshot.bookShopId(),
                snapshot.rentalMethodId(),
                snapshot.isReturned(),
                snapshot.startDate(),
                snapshot.endDate(),
                true,
                true,
                0,
                insertedRows
        );
    }

    private BookRentalSnapshot readSnapshot(long rentalId) {
        var rentalRow = jdbcTemplate.query(
                SELECT_RENTAL_BY_ID,
                rs -> rs.next() ? new Object[]{
                        rs.getLong("book_id"),
                        rs.getLong("user_id"),
                        rs.getLong("employee_id"),
                        rs.getLong("book_shop_id"),
                        rs.getLong("rental_method_id"),
                        rs.getInt("is_returned") != 0,
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate()
                } : null,
                rentalId
        );
        if (rentalRow == null) {
            throw new IllegalArgumentException("Nie znaleziono wypożyczenia o id=" + rentalId);
        }

        return new BookRentalSnapshot(
                (Long) rentalRow[0],
                (Long) rentalRow[1],
                (Long) rentalRow[2],
                (Long) rentalRow[3],
                (Long) rentalRow[4],
                (Boolean) rentalRow[5],
                (LocalDate) rentalRow[6],
                (LocalDate) rentalRow[7]
        );
    }
}