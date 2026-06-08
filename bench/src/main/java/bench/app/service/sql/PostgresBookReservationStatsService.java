package bench.app.service.sql;

import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserReservationRentalCount;
import bench.app.repository.sql.postgres.PostgresBookReservationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class PostgresBookReservationStatsService {
    private static final String USERS_ACTIVITY_COUNTS_GLOBAL = """
            WITH reservation_counts AS (
                SELECT br.userid, COUNT(*) AS reservation_count
                FROM bench.bookreservation br
                GROUP BY br.userid
            ),
            rental_counts AS (
                SELECT rt.userid, COUNT(*) AS rental_count
                FROM bench.bookrental rt
                GROUP BY rt.userid
            )
            SELECT u.id,
                   u.name,
                   u.surname,
                   COALESCE(rc.reservation_count, 0) AS reservation_count,
                   COALESCE(rtc.rental_count, 0) AS rental_count,
                   COALESCE(rc.reservation_count, 0) + COALESCE(rtc.rental_count, 0) AS total_count
            FROM bench.bookshopuser u
            LEFT JOIN reservation_counts rc ON rc.userid = u.id
            LEFT JOIN rental_counts rtc ON rtc.userid = u.id
            WHERE COALESCE(rc.reservation_count, 0) > 0
               OR COALESCE(rtc.rental_count, 0) > 0
            ORDER BY total_count DESC,
                     reservation_count DESC,
                     rental_count DESC,
                     u.id ASC
            """;

    private final PostgresBookReservationRepository bookReservationRepository;
    private final JdbcTemplate jdbcTemplate;

    public PostgresBookReservationStatsService(
            PostgresBookReservationRepository bookReservationRepository,
            @Qualifier("postgresDataSource") DataSource dataSource
    ) {
        this.bookReservationRepository = bookReservationRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<UserReservationRentalCount> getUsersActivityCountsByShop(long shopId) {
        return bookReservationRepository.findUsersActivityCountsByShop((int) shopId);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<UserReservationRentalCount> getUsersActivityCountsGlobal() {
        return jdbcTemplate.query(
            USERS_ACTIVITY_COUNTS_GLOBAL,
            (rs, rowNum) -> new UserReservationRentalCount(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("surname"),
                rs.getLong("reservation_count"),
                rs.getLong("rental_count"),
                rs.getLong("total_count")
            )
        );
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<EngagedUser> getEngagedUsersByShopAndPeriod(long shopId, LocalDate fromDate, LocalDate toDate) {
        return bookReservationRepository.findEngagedUsersByShopAndPeriod(
                (int) shopId,
                Date.valueOf(fromDate),
                Date.valueOf(toDate)
        );
    }
}
