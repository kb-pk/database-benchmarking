package bench.app.service.sql;

import bench.app.model.common.BookRentalCloseOverdueResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.List;

@Service
public class MssqlBookRentalUpdateService {
    private static final String SELECT_OVERDUE_OPEN_RENTALS = """
            SELECT br.id, br.isReturned, br.endDate
            FROM bench.BookRental br
                        WHERE br.isReturned = 0
              AND br.startDate < DATEADD(day, -?, CAST(GETDATE() AS date))
            """;

    private static final String CLOSE_OVERDUE_OPEN_RENTALS = """
            UPDATE bench.BookRental
            SET isReturned = 1,
                endDate = CAST(GETDATE() AS date)
                        WHERE isReturned = 0
              AND startDate < DATEADD(day, -?, CAST(GETDATE() AS date))
            """;

    private static final String RESTORE_RENTAL_STATE_BY_ID = """
            UPDATE bench.BookRental
            SET isReturned = ?,
                endDate = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookRentalUpdateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookRentalCloseOverdueResult closeOverdueRentals(int daysThreshold, boolean restoreAfterUpdate) {
        if (daysThreshold <= 0) {
            throw new IllegalArgumentException("daysThreshold musi być większe od 0");
        }

        List<RentalState> matchedRentals = jdbcTemplate.query(
                SELECT_OVERDUE_OPEN_RENTALS,
                (rs, rowNum) -> new RentalState(
                        rs.getLong("id"),
                        rs.getInt("isReturned"),
                        rs.getDate("endDate")
                ),
                daysThreshold
        );

        int closedRentals = jdbcTemplate.update(CLOSE_OVERDUE_OPEN_RENTALS, daysThreshold);

        int restoredRentals = 0;
        if (restoreAfterUpdate && !matchedRentals.isEmpty()) {
            for (RentalState state : matchedRentals) {
                restoredRentals += jdbcTemplate.update(
                        RESTORE_RENTAL_STATE_BY_ID,
                        state.isReturned,
                        state.endDate,
                        state.id
                );
            }
        }

        return new BookRentalCloseOverdueResult(
                daysThreshold,
                matchedRentals.size(),
                closedRentals,
                restoredRentals,
                restoreAfterUpdate
        );
    }

    private record RentalState(long id, int isReturned, Date endDate) {
    }
}
