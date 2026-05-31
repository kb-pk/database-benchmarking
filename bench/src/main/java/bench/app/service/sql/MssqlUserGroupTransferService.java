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
public class MssqlUserGroupTransferService {
    private static final String CHECK_SHOP_EXISTS = """
            SELECT COUNT(*)
            FROM bench.BookShop
            WHERE id = ?
            """;

    private static final String SELECT_USERS_TO_MOVE = """
            SELECT TOP (?) u.id
            FROM bench.BookShopUser u
            WHERE u.mainBookShopId = ?
            ORDER BY u.id
            """;

    private static final String MOVE_USERS_TO_TARGET_SHOP = """
            UPDATE bench.BookShopUser
            SET mainBookShopId = :targetShopId
            WHERE id IN (:userIds)
            """;

    private static final String RESTORE_USERS_TO_SOURCE_SHOP = """
            UPDATE bench.BookShopUser
            SET mainBookShopId = :sourceShopId
            WHERE id IN (:userIds)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    public MssqlUserGroupTransferService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
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
                SELECT_USERS_TO_MOVE,
                (rs, rowNum) -> rs.getLong("id"),
                maxUsers,
                sourceShopId
        );

        int movedUsers = 0;
        if (!movedUserIds.isEmpty()) {
            MapSqlParameterSource moveParams = new MapSqlParameterSource()
                    .addValue("targetShopId", targetShopId)
                    .addValue("userIds", movedUserIds);
            movedUsers = namedParameterJdbcTemplate.update(MOVE_USERS_TO_TARGET_SHOP, moveParams);
        }

        int restoredUsers = 0;
        if (restoreAfterUpdate && !movedUserIds.isEmpty()) {
            MapSqlParameterSource restoreParams = new MapSqlParameterSource()
                    .addValue("sourceShopId", sourceShopId)
                    .addValue("userIds", movedUserIds);
            restoredUsers = namedParameterJdbcTemplate.update(RESTORE_USERS_TO_SOURCE_SHOP, restoreParams);
        }

        return new UserGroupShopTransferResult(
                sourceShopId,
                targetShopId,
                maxUsers,
                movedUsers,
                restoredUsers,
                restoreAfterUpdate
        );
    }
}