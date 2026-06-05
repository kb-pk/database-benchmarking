package bench.app.service.sql;

import bench.app.benchmark.EmployeeRentalDaySnapshot;
import bench.app.benchmark.EmployeeRentalDaySnapshotStore;
import bench.app.benchmark.RequestTimingContextHolder;
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
public class PostgresEmployeeRentalDayDeleteService {
    private static final String DB_ENGINE = "POSTGRESQL";

    private static final String SELECT_MATCHING_RENTALS = """
            SELECT br.id AS rental_id,
                   br.bookid AS book_id,
                   br.userid AS user_id,
                   br.employeeid AS employee_id,
                   br.bookshopid AS book_shop_id,
                   br.rentalmethodid AS rental_method_id,
                   br.isreturned AS is_returned,
                   br.startdate AS start_date,
                   br.enddate AS end_date
            FROM bench.bookrental br
            WHERE br.employeeid = ?
              AND br.startdate = ?
            ORDER BY br.id
            """;

    private static final String DELETE_MATCHING_RENTALS = """
            DELETE FROM bench.bookrental
            WHERE employeeid = ?
              AND startdate = ?
            """;

    private static final String INSERT_RENTAL = """
                        INSERT INTO bench.bookrental (
                                id,
                                bookid,
                                userid,
                                employeeid,
                                bookshopid,
                                isreturned,
                                startdate,
                                enddate,
                                rentalmethodid
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final EmployeeRentalDaySnapshotStore snapshotStore;
        private final RequestTimingContextHolder timingContextHolder;

    public PostgresEmployeeRentalDayDeleteService(
            @Qualifier("postgresDataSource") DataSource dataSource,
                        EmployeeRentalDaySnapshotStore snapshotStore,
                        RequestTimingContextHolder timingContextHolder
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
                this.timingContextHolder = timingContextHolder;
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public EmployeeRentalDayDeleteResult deleteRentalsByEmployeeAndDay(
            long employeeId,
            LocalDate rentalDate,
            boolean restoreAfterDelete
    ) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(employeeId, rentalDate);
        }

        List<EmployeeRentalDaySnapshot> snapshotRows = timingContextHolder.excludeFromTiming(() -> loadMatchingRentals(employeeId, rentalDate));
        List<EmployeeRentalDaySnapshot> matchedRows = loadMatchingRentals(employeeId, rentalDate);

        int deletedRows = jdbcTemplate.update(
                DELETE_MATCHING_RENTALS,
                employeeId,
                Date.valueOf(rentalDate)
        );

        timingContextHolder.excludeFromTiming(() -> snapshotStore.save(DB_ENGINE, employeeId, rentalDate, snapshotRows));

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

        private List<EmployeeRentalDaySnapshot> loadMatchingRentals(long employeeId, LocalDate rentalDate) {
                return jdbcTemplate.query(
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
        }
}