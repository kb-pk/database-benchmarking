package bench.app.service.sql;

import bench.app.model.common.UserActivationBulkUpdateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
public class MssqlUserActivationUpdateService {
    private static final String SELECT_INACTIVE_STATUS_ID = """
            SELECT TOP 1 a.id
            FROM bench.ActivationStatus a
            WHERE UPPER(LTRIM(RTRIM(REPLACE(ISNULL(a.status, ''), CHAR(13), '')))) = 'INACTIVE'
            ORDER BY a.id
            """;

        private static final String SELECT_ACTIVE_STATUS_ID = """
            SELECT TOP 1 a.id
            FROM bench.ActivationStatus a
            WHERE UPPER(LTRIM(RTRIM(REPLACE(ISNULL(a.status, ''), CHAR(13), '')))) = 'ACTIVE'
            ORDER BY a.id
            """;

    private static final String SELECT_ELIGIBLE_ACTIVE_USERS = """
            SELECT u.id, u.isActiveId
            FROM bench.BookShopUser u
                        WHERE u.isActiveId = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookRental br
                  WHERE br.userId = u.id
                                        AND br.isReturned = 0
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookReservation r
                  WHERE r.userId = u.id
              )
            ORDER BY u.id
            """;

    private static final String UPDATE_ELIGIBLE_USERS_TO_INACTIVE = """
            UPDATE u
            SET u.isActiveId = ?
            FROM bench.BookShopUser u
                        WHERE u.isActiveId = ?
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookRental br
                  WHERE br.userId = u.id
                                        AND br.isReturned = 0
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookReservation r
                  WHERE r.userId = u.id
              )
            """;

    private static final String RESTORE_USER_ACTIVE_STATUS = """
            UPDATE bench.BookShopUser
            SET isActiveId = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlUserActivationUpdateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public UserActivationBulkUpdateResult setUsersInactiveIfNoOpenRentalOrReservation(boolean restoreAfterUpdate) {
        Integer inactiveStatusId = jdbcTemplate.query(
                SELECT_INACTIVE_STATUS_ID,
                rs -> rs.next() ? rs.getInt("id") : null
        );
        if (inactiveStatusId == null) {
            throw new IllegalArgumentException("Nie znaleziono statusu INACTIVE w bench.ActivationStatus");
        }

        Integer activeStatusId = jdbcTemplate.query(
            SELECT_ACTIVE_STATUS_ID,
            rs -> rs.next() ? rs.getInt("id") : null
        );
        if (activeStatusId == null) {
            throw new IllegalArgumentException("Nie znaleziono statusu ACTIVE w bench.ActivationStatus");
        }

        List<UserState> matchedUsers = jdbcTemplate.query(
                SELECT_ELIGIBLE_ACTIVE_USERS,
            (rs, rowNum) -> new UserState(rs.getLong("id"), rs.getLong("isActiveId")),
            activeStatusId
        );

        int updatedUsers = 0;
        if (!matchedUsers.isEmpty()) {
            updatedUsers = jdbcTemplate.update(UPDATE_ELIGIBLE_USERS_TO_INACTIVE, inactiveStatusId, activeStatusId);
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
