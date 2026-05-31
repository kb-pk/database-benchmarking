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
public class PostgresBookRentalUpdateService {
    private static final String SELECT_OVERDUE_OPEN_RENTALS = """
            SELECT br.id, br.isreturned, br.enddate
            FROM bench.bookrental br
            WHERE COALESCE(br.isreturned, 0) = 0
              AND br.startdate < (CURRENT_DATE - (? * INTERVAL '1 day'))
            """;

    private static final String CLOSE_OVERDUE_OPEN_RENTALS = """
            UPDATE bench.bookrental br
            SET isreturned = 1,
                enddate = CURRENT_DATE
            WHERE COALESCE(br.isreturned, 0) = 0
              AND br.startdate < (CURRENT_DATE - (? * INTERVAL '1 day'))
            """;

    private static final String RESTORE_RENTAL_STATE_BY_ID = """
            UPDATE bench.bookrental
            SET isreturned = ?,
                enddate = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresBookRentalUpdateService(@Qualifier("postgresDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public BookRentalCloseOverdueResult closeOverdueRentals(int daysThreshold, boolean restoreAfterUpdate) {
        if (daysThreshold <= 0) {
            throw new IllegalArgumentException("daysThreshold musi być większe od 0");
        }

        List<RentalState> matchedRentals = jdbcTemplate.query(
                SELECT_OVERDUE_OPEN_RENTALS,
                (rs, rowNum) -> new RentalState(
                        rs.getLong("id"),
                        rs.getInt("isreturned"),
                        rs.getDate("enddate")
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