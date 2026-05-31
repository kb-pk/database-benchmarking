package bench.app.service.sql;

import bench.app.model.common.UserPermissionUpdateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class PostgresUserAccountUpdateService {
    private static final String SELECT_PREVIOUS_PERMISSION_ID = """
            SELECT ua.permissionsid
            FROM bench.useraccount ua
            WHERE ua.userid = ?
            ORDER BY ua.id
            LIMIT 1
            """;

    private static final String UPDATE_PERMISSION_ID_BY_USER_ID = """
            UPDATE bench.useraccount
            SET permissionsid = ?
            WHERE userid = ?
            """;

    private static final String CHECK_PERMISSION_EXISTS = """
            SELECT COUNT(*)
            FROM bench.useraccountpermissions
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresUserAccountUpdateService(@Qualifier("postgresDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public UserPermissionUpdateResult updateUserPermissions(long userId, long permissionsId, boolean restoreAfterUpdate) {
        Integer previousPermissionsId = jdbcTemplate.query(
                SELECT_PREVIOUS_PERMISSION_ID,
                rs -> rs.next() ? rs.getInt("permissionsid") : null,
                userId
        );

        if (previousPermissionsId == null) {
            throw new IllegalArgumentException("Nie znaleziono konta użytkownika dla userId=" + userId);
        }

        Integer permissionsCount = jdbcTemplate.queryForObject(
                CHECK_PERMISSION_EXISTS,
                Integer.class,
                permissionsId
        );
        if (permissionsCount == null || permissionsCount == 0) {
            throw new IllegalArgumentException("Nie znaleziono uprawnienia permissionsId=" + permissionsId);
        }

        int affectedRows = jdbcTemplate.update(UPDATE_PERMISSION_ID_BY_USER_ID, permissionsId, userId);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("Aktualizacja nie objęła żadnego konta dla userId=" + userId);
        }

        long finalPermissionsId = permissionsId;
        if (restoreAfterUpdate) {
            jdbcTemplate.update(UPDATE_PERMISSION_ID_BY_USER_ID, previousPermissionsId, userId);
            finalPermissionsId = previousPermissionsId;
        }

        return new UserPermissionUpdateResult(
                userId,
                previousPermissionsId,
                permissionsId,
                finalPermissionsId,
                restoreAfterUpdate,
                affectedRows
        );
    }
}