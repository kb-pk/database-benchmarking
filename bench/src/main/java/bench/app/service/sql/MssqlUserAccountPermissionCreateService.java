package bench.app.service.sql;

import bench.app.model.common.UserPermissionCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
public class MssqlUserAccountPermissionCreateService {
    private static final String SELECT_NEXT_PERMISSION_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.UserAccountPermissions
            """;

    private static final String INSERT_PERMISSION = """
            INSERT INTO bench.UserAccountPermissions (id, permission, details)
            VALUES (?, ?, ?)
            """;

    private static final String DELETE_PERMISSION_BY_ID = """
            DELETE FROM bench.UserAccountPermissions
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlUserAccountPermissionCreateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public UserPermissionCreateResult createPermission(String permission, String details, boolean restoreAfterCreate) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("permission nie może być puste");
        }

        Long createdPermissionsId = jdbcTemplate.query(
                SELECT_NEXT_PERMISSION_ID,
                rs -> rs.next() ? rs.getLong("next_id") : null
        );
        if (createdPermissionsId == null) {
            throw new IllegalArgumentException("Nie udało się ustalić nowego id uprawnienia");
        }

        int insertedRows = jdbcTemplate.update(INSERT_PERMISSION, createdPermissionsId, permission, details);
        int deletedRows = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            deletedRows = jdbcTemplate.update(DELETE_PERMISSION_BY_ID, createdPermissionsId);
            existsAfterOperation = false;
        }

        return new UserPermissionCreateResult(
                createdPermissionsId,
                permission,
                details,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

}