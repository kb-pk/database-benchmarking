package bench.app.service.sql;

import bench.app.model.common.UserGroupShopTransferResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
public class PostgresUserGroupTransferService {
    private static final String CHECK_SHOP_EXISTS = """
            SELECT COUNT(*)
            FROM bench.bookshop
            WHERE id = ?
            """;

    private static final String MOVE_USERS_TO_TARGET_SHOP = """
            WITH candidates AS (
                SELECT u.id
                FROM bench.bookshopuser u
                WHERE u.mainbookshopid = ?
                ORDER BY u.id
                LIMIT ?
            )
            UPDATE bench.bookshopuser u
            SET mainbookshopid = ?
            FROM candidates c
            WHERE u.id = c.id
            RETURNING u.id
            """;

    private static final String RESTORE_USERS_TO_SOURCE_SHOP = """
            UPDATE bench.bookshopuser
            SET mainbookshopid = :sourceShopId
            WHERE id IN (:userIds)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public PostgresUserGroupTransferService(@Qualifier("postgresDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "postgresTransactionManager")
    public UserGroupShopTransferResult transferUserGroup(
            long sourceShopId,
            long targetShopId,
            int maxUsers,
            boolean restoreAfterUpdate
    ) {
        if (maxUsers <= 0) {
            throw new IllegalArgumentException("maxUsers musi być większe od 0");
        }
        if (sourceShopId == targetShopId) {
            throw new IllegalArgumentException("sourceShopId i targetShopId muszą być różne");
        }

        Integer sourceCount = jdbcTemplate.queryForObject(CHECK_SHOP_EXISTS, Integer.class, sourceShopId);
        Integer targetCount = jdbcTemplate.queryForObject(CHECK_SHOP_EXISTS, Integer.class, targetShopId);
        if (sourceCount == null || sourceCount == 0) {
            throw new IllegalArgumentException("Nie znaleziono sourceShopId=" + sourceShopId);
        }
        if (targetCount == null || targetCount == 0) {
            throw new IllegalArgumentException("Nie znaleziono targetShopId=" + targetShopId);
        }

        List<Long> movedUserIds = jdbcTemplate.query(
                MOVE_USERS_TO_TARGET_SHOP,
                (rs, rowNum) -> rs.getLong("id"),
                sourceShopId,
                maxUsers,
                targetShopId
        );

        int restoredUsers = 0;
        if (restoreAfterUpdate && !movedUserIds.isEmpty()) {
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("sourceShopId", sourceShopId)
                    .addValue("userIds", movedUserIds);
            restoredUsers = namedParameterJdbcTemplate.update(RESTORE_USERS_TO_SOURCE_SHOP, params);
        }

        return new UserGroupShopTransferResult(
                sourceShopId,
                targetShopId,
                maxUsers,
                movedUserIds.size(),
                restoredUsers,
                restoreAfterUpdate
        );
    }
}