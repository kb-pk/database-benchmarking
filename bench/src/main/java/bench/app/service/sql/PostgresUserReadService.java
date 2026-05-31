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
public class PostgresUserReadService {
    private static final String QUERY = """
            SELECT u.id, u.name, u.surname, u.phonenumber, u.email, s.status
            FROM bench.bookshopuser u
            JOIN bench.activationstatus s ON s.id = u.isactiveid
            WHERE u.mainbookshopid = ?
              AND UPPER(s.status) = 'ACTIVE'
            ORDER BY u.id
            """;

    private static final String ENGAGED_USERS_QUERY = """
            SELECT u.id, u.name, u.surname, u.phonenumber, u.email
            FROM bench.bookshopuser u
            WHERE EXISTS (
                SELECT 1
                FROM bench.bookreservation br
                WHERE br.userid = u.id
                  AND br.whenreserved BETWEEN ? AND ?
            )
            AND EXISTS (
                SELECT 1
                FROM bench.bookrental rt
                WHERE rt.userid = u.id
                  AND rt.startdate BETWEEN ? AND ?
            )
            ORDER BY u.id
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserReadService(@Qualifier("postgresDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public List<ActiveUser> getActiveUsersByShopId(long shopId) {
        return jdbcTemplate.query(
                QUERY,
                (rs, rowNum) -> new ActiveUser(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("surname"),
                        rs.getString("phonenumber"),
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
                        rs.getString("phonenumber"),
                        rs.getString("email")
                ),
                    from, to,
                    from, to
        );
    }
}
