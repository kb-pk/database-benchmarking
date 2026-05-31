package bench.app.service.sql;

import bench.app.model.common.BookShopCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
public class MssqlBookShopCreateService {
    private static final String SELECT_NEXT_BOOKSHOP_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.BookShop
            """;

    private static final String INSERT_BOOKSHOP = """
            INSERT INTO bench.BookShop (id, shopName, address, email, managerId, openingHoursId)
            VALUES (?, ?, ?, ?, ?, NULL)
            """;

    private static final String DELETE_BOOKSHOP_BY_ID = """
            DELETE FROM bench.BookShop
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookShopCreateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookShopCreateResult createBookShop(String shopName, String address, String email, long managerId, boolean restoreAfterCreate) {
        if (shopName == null || shopName.isBlank()) {
            throw new IllegalArgumentException("shopName nie może być puste");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address nie może być puste");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email nie może być pusty");
        }

        Long createdBookShopId = jdbcTemplate.query(
                SELECT_NEXT_BOOKSHOP_ID,
                rs -> rs.next() ? rs.getLong("next_id") : null
        );
        if (createdBookShopId == null) {
            throw new IllegalArgumentException("Nie udało się ustalić nowego id sklepu");
        }

        int insertedRows = jdbcTemplate.update(INSERT_BOOKSHOP, createdBookShopId, shopName, address, email, managerId);
        int deletedRows = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            deletedRows = jdbcTemplate.update(DELETE_BOOKSHOP_BY_ID, createdBookShopId);
            existsAfterOperation = false;
        }

        return new BookShopCreateResult(
                String.valueOf(createdBookShopId),
                shopName,
                address,
                email,
                String.valueOf(managerId),
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }
}