package bench.app.service.sql;

import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserReservationRentalCount;
import bench.app.repository.sql.mssql.MssqlBookReservationRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class MssqlBookReservationStatsService {
    private final MssqlBookReservationRepository bookReservationRepository;
    private final JdbcTemplate jdbcTemplate;

    private static final String USERS_ACTIVITY_COUNTS_GLOBAL_SQL = """
        SELECT
            u.id AS userId,
            u.name,
            u.surname,
            COALESCE(br.reservationCount, 0) AS reservationCount,
            COALESCE(rt.rentalCount, 0) AS rentalCount,
            COALESCE(br.reservationCount, 0) + COALESCE(rt.rentalCount, 0) AS totalCount
        FROM bench.BookShopUser u
        LEFT JOIN (
            SELECT userId, COUNT_BIG(*) AS reservationCount
            FROM bench.BookReservation
            GROUP BY userId
        ) br ON br.userId = u.id
        LEFT JOIN (
            SELECT userId, COUNT_BIG(*) AS rentalCount
            FROM bench.BookRental
            GROUP BY userId
        ) rt ON rt.userId = u.id
        WHERE COALESCE(br.reservationCount, 0) > 0 OR COALESCE(rt.rentalCount, 0) > 0
        ORDER BY totalCount DESC, u.id ASC
        """;

    public MssqlBookReservationStatsService(
            MssqlBookReservationRepository bookReservationRepository,
            @Qualifier("mssqlDataSource") DataSource dataSource) {
        this.bookReservationRepository = bookReservationRepository;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<UserReservationRentalCount> getUsersActivityCountsByShop(long shopId) {
        return bookReservationRepository.findUsersActivityCountsByShop((int) shopId);
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<UserReservationRentalCount> getUsersActivityCountsGlobal() {
        return jdbcTemplate.query(
            USERS_ACTIVITY_COUNTS_GLOBAL_SQL,
            (rs, rowNum) -> new UserReservationRentalCount(
                rs.getLong("userId"),
                rs.getString("name"),
                rs.getString("surname"),
                rs.getLong("reservationCount"),
                rs.getLong("rentalCount"),
                rs.getLong("totalCount")
            )
        );
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<EngagedUser> getEngagedUsersByShopAndPeriod(long shopId, LocalDate fromDate, LocalDate toDate) {
        return bookReservationRepository.findEngagedUsersByShopAndPeriod(
                (int) shopId,
                Date.valueOf(fromDate),
                Date.valueOf(toDate)
        );
    }
}
