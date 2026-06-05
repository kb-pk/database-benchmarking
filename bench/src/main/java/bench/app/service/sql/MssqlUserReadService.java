package bench.app.service.sql;

import bench.app.model.common.ActiveUser;
import bench.app.model.common.EngagedUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class MssqlUserReadService {
    private static final String QUERY = """
            SELECT u.id, u.name, u.surname, u.phoneNumber, u.email, s.status
            FROM bench.BookShopUser u
            JOIN bench.ActivationStatus s ON s.id = u.isActiveId
            WHERE u.mainBookShopId = ?
                            AND UPPER(LTRIM(RTRIM(REPLACE(ISNULL(s.status, ''), CHAR(13), '')))) = 'ACTIVE'
            ORDER BY u.id
            """;

    private static final String ENGAGED_USERS_QUERY = """
            SELECT u.id, u.name, u.surname, u.phoneNumber, u.email
            FROM bench.BookShopUser u
            WHERE EXISTS (
                SELECT 1
                FROM bench.BookReservation br
                WHERE br.userId = u.id
                  AND br.whenReserved BETWEEN ? AND ?
            )
            AND EXISTS (
                SELECT 1
                FROM bench.BookRental rt
                WHERE rt.userId = u.id
                  AND rt.startDate BETWEEN ? AND ?
            )
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlUserReadService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<ActiveUser> getActiveUsersByShopId(long shopId) {
        return jdbcTemplate.query(
                QUERY,
                (rs, rowNum) -> new ActiveUser(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phoneNumber"),
                        rs.getString("email"),
                        rs.getString("status")
                ),
                shopId
        );
    }

    public List<EngagedUser> getEngagedUsersByPeriod(LocalDate fromDate, LocalDate toDate) {
        Date from = Date.valueOf(fromDate);
        Date to = Date.valueOf(toDate);
        return jdbcTemplate.query(
                ENGAGED_USERS_QUERY,
                (rs, rowNum) -> new EngagedUser(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phoneNumber"),
                        rs.getString("email")
                ),
                    from, to,
                    from, to
        );
    }
}
