package bench.app.service.sql;

import bench.app.model.common.UserActivationBulkUpdateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
public class PostgresUserActivationUpdateService {
    private static final String SELECT_INACTIVE_STATUS_ID = """
            SELECT a.id
            FROM bench.activationstatus a
            WHERE UPPER(a.status) = 'INACTIVE'
            ORDER BY a.id
            LIMIT 1
            """;

    private static final String SELECT_ELIGIBLE_ACTIVE_USERS = """
            SELECT u.id, u.isactiveid
            FROM bench.bookshopuser u
            JOIN bench.activationstatus a ON a.id = u.isactiveid
            WHERE UPPER(a.status) = 'ACTIVE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookrental br
                  WHERE br.userid = u.id
                    AND COALESCE(br.isreturned, 0) = 0
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookreservation r
                  WHERE r.userid = u.id
              )
            ORDER BY u.id
            """;

    private static final String UPDATE_ELIGIBLE_USERS_TO_INACTIVE = """
            UPDATE bench.bookshopuser u
            SET isactiveid = ?
            WHERE EXISTS (
                SELECT 1
                FROM bench.activationstatus a
                WHERE a.id = u.isactiveid
                  AND UPPER(a.status) = 'ACTIVE'
            )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookrental br
                  WHERE br.userid = u.id
                    AND COALESCE(br.isreturned, 0) = 0
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookreservation r
                  WHERE r.userid = u.id
              )
            """;

    private static final String RESTORE_USER_ACTIVE_STATUS = """
            UPDATE bench.bookshopuser
            SET isactiveid = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserActivationUpdateService(@Qualifier("postgresDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public UserActivationBulkUpdateResult setUsersInactiveIfNoOpenRentalOrReservation(boolean restoreAfterUpdate) {
        Integer inactiveStatusId = jdbcTemplate.query(
                SELECT_INACTIVE_STATUS_ID,
                rs -> rs.next() ? rs.getInt("id") : null
        );
        if (inactiveStatusId == null) {
            throw new IllegalArgumentException("Nie znaleziono statusu INACTIVE w bench.activationstatus");
        }

        List<UserState> matchedUsers = jdbcTemplate.query(
                SELECT_ELIGIBLE_ACTIVE_USERS,
                (rs, rowNum) -> new UserState(rs.getLong("id"), rs.getLong("isactiveid"))
        );

        int updatedUsers = 0;
        if (!matchedUsers.isEmpty()) {
            updatedUsers = jdbcTemplate.update(UPDATE_ELIGIBLE_USERS_TO_INACTIVE, inactiveStatusId);
        }

        int restoredUsers = 0;
        if (restoreAfterUpdate && !matchedUsers.isEmpty()) {
            for (UserState state : matchedUsers) {
                restoredUsers += jdbcTemplate.update(RESTORE_USER_ACTIVE_STATUS, state.previousStatusId, state.userId);
            }
        }

        return new UserActivationBulkUpdateResult(
                inactiveStatusId,
                matchedUsers.size(),
                updatedUsers,
                restoredUsers,
                restoreAfterUpdate
        );
    }

    private record UserState(long userId, long previousStatusId) {
    }
}
