package bench.app.service.sql;

import bench.app.benchmark.EmployeeRentalDaySnapshot;
import bench.app.benchmark.EmployeeRentalDaySnapshotStore;
import bench.app.model.common.EmployeeRentalDayDeleteResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class MssqlEmployeeRentalDayDeleteService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_MATCHING_RENTALS = """
            SELECT br.id AS rental_id,
                   br.bookId AS book_id,
                   br.userId AS user_id,
                   br.employeeId AS employee_id,
                   br.bookShopId AS book_shop_id,
                   br.rentalMethodId AS rental_method_id,
                   br.isReturned AS is_returned,
                   br.startDate AS start_date,
                   br.endDate AS end_date
            FROM bench.BookRental br
            WHERE br.employeeId = ?
              AND br.startDate = ?
            ORDER BY br.id
            """;

    private static final String DELETE_MATCHING_RENTALS = """
            DELETE FROM bench.BookRental
            WHERE employeeId = ?
              AND startDate = ?
            """;

    private static final String INSERT_RENTAL = """
            INSERT INTO bench.BookRental (id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmployeeRentalDaySnapshotStore snapshotStore;

    public MssqlEmployeeRentalDayDeleteService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            EmployeeRentalDaySnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public EmployeeRentalDayDeleteResult deleteRentalsByEmployeeAndDay(
            long employeeId,
            LocalDate rentalDate,
            boolean restoreAfterDelete
    ) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(employeeId, rentalDate);
        }

        List<EmployeeRentalDaySnapshot> matchedRows = jdbcTemplate.query(
                SELECT_MATCHING_RENTALS,
                (rs, rowNum) -> new EmployeeRentalDaySnapshot(
                        rs.getLong("rental_id"),
                        rs.getLong("book_id"),
                        rs.getLong("user_id"),
                        rs.getLong("employee_id"),
                        rs.getLong("book_shop_id"),
                        rs.getLong("rental_method_id"),
                        rs.getInt("is_returned") != 0,
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date") == null ? null : rs.getDate("end_date").toLocalDate()
                ),
                employeeId,
                Date.valueOf(rentalDate)
        );

        int deletedRows = jdbcTemplate.update(
                DELETE_MATCHING_RENTALS,
                employeeId,
                Date.valueOf(rentalDate)
        );

        snapshotStore.save(DB_ENGINE, employeeId, rentalDate, matchedRows);

        return new EmployeeRentalDayDeleteResult(
                employeeId,
                rentalDate,
                matchedRows.size(),
                deletedRows,
                0,
                false
        );
    }

    private EmployeeRentalDayDeleteResult restoreFromSnapshot(long employeeId, LocalDate rentalDate) {
        List<EmployeeRentalDaySnapshot> snapshotRows = snapshotStore.find(DB_ENGINE, employeeId, rentalDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Brak snapshotu D6 dla employeeId=" + employeeId + " rentalDate=" + rentalDate
                ));

        int restoredRows = 0;
        for (EmployeeRentalDaySnapshot row : snapshotRows) {
            restoredRows += jdbcTemplate.update(
                    INSERT_RENTAL,
                    row.rentalId(),
                    row.bookId(),
                    row.userId(),
                    row.employeeId(),
                    row.bookShopId(),
                    row.isReturned() ? 1 : 0,
                    Date.valueOf(row.startDate()),
                    row.endDate() == null ? null : Date.valueOf(row.endDate()),
                    row.rentalMethodId()
            );
        }

        snapshotStore.remove(DB_ENGINE, employeeId, rentalDate);

        return new EmployeeRentalDayDeleteResult(
                employeeId,
                rentalDate,
                snapshotRows.size(),
                0,
                restoredRows,
                true
        );
    }
}