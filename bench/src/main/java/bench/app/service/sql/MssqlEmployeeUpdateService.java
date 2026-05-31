package bench.app.service.sql;

import bench.app.model.common.EmployeeShopAssignmentUpdateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

@Service
public class MssqlEmployeeUpdateService {
    private static final String SELECT_PREVIOUS_SHOP_ID = """
            SELECT e.primaryBookShopId
            FROM bench.Employee e
            WHERE e.id = ?
            """;

    private static final String CHECK_SHOP_EXISTS = """
            SELECT COUNT(*)
            FROM bench.BookShop bs
            WHERE bs.id = ?
            """;

    private static final String UPDATE_EMPLOYEE_SHOP = """
            UPDATE bench.Employee
            SET primaryBookShopId = ?
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlEmployeeUpdateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public EmployeeShopAssignmentUpdateResult reassignEmployeeToShop(long employeeId, long newShopId, boolean restoreAfterUpdate) {
        Integer previousShopId = jdbcTemplate.query(
                SELECT_PREVIOUS_SHOP_ID,
                rs -> rs.next() ? rs.getInt("primaryBookShopId") : null,
                employeeId
        );
        if (previousShopId == null) {
            throw new IllegalArgumentException("Nie znaleziono pracownika employeeId=" + employeeId);
        }

        Integer targetShopCount = jdbcTemplate.queryForObject(CHECK_SHOP_EXISTS, Integer.class, newShopId);
        if (targetShopCount == null || targetShopCount == 0) {
            throw new IllegalArgumentException("Nie znaleziono sklepu shopId=" + newShopId);
        }

        int affectedRows = jdbcTemplate.update(UPDATE_EMPLOYEE_SHOP, newShopId, employeeId);
        if (affectedRows == 0) {
            throw new IllegalArgumentException("Aktualizacja nie objęła żadnego pracownika employeeId=" + employeeId);
        }

        long finalShopId = newShopId;
        if (restoreAfterUpdate) {
            jdbcTemplate.update(UPDATE_EMPLOYEE_SHOP, previousShopId, employeeId);
            finalShopId = previousShopId;
        }

        return new EmployeeShopAssignmentUpdateResult(
                employeeId,
                previousShopId,
                newShopId,
                finalShopId,
                restoreAfterUpdate,
                affectedRows
        );
    }
}
