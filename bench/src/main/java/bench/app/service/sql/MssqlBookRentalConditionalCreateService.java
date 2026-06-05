package bench.app.service.sql;

import bench.app.model.common.BookRentalConditionalCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;

@Service
public class MssqlBookRentalConditionalCreateService {
    private static final String SELECT_NEXT_RENTAL_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.BookRental
            """;

    private static final String CHECK_USER_ACTIVE = """
            SELECT COUNT(*)
            FROM bench.BookShopUser u
            JOIN bench.ActivationStatus s ON s.id = u.isActiveId
            WHERE u.id = ?
                            AND UPPER(LTRIM(RTRIM(REPLACE(ISNULL(s.status, ''), CHAR(13), '')))) = 'ACTIVE'
            """;

    private static final String CHECK_BOOK_IN_SHOP = """
            SELECT COUNT(*)
            FROM bench.Book
            WHERE id = ?
              AND bookShopId = ?
            """;

    private static final String SELECT_EMPLOYEE_IN_SHOP = """
            SELECT TOP 1 id
            FROM bench.Employee
            WHERE primaryBookShopId = ?
            ORDER BY id
            """;

    private static final String SELECT_RENTAL_METHOD_ID = """
            SELECT TOP 1 id
            FROM bench.BookRentalMethod
            ORDER BY id
            """;

    private static final String INSERT_RENTAL = """
            INSERT INTO bench.BookRental (id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId)
            VALUES (?, ?, ?, ?, ?, 0, ?, NULL, ?)
            """;

    private static final String DELETE_RENTAL = """
            DELETE FROM bench.BookRental
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookRentalConditionalCreateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookRentalConditionalCreateResult createRentalIfValid(long shopId, long bookId, long userId, LocalDate startDate, boolean restoreAfterCreate) {
        if (!exists(CHECK_USER_ACTIVE, userId)) {
            throw new IllegalArgumentException("Użytkownik nie jest aktywny albo nie istnieje");
        }
        if (!exists(CHECK_BOOK_IN_SHOP, bookId, shopId)) {
            throw new IllegalArgumentException("Książka nie należy do wskazanego sklepu");
        }

        Long employeeId = queryLong(SELECT_EMPLOYEE_IN_SHOP, "Brak pracownika przypisanego do wskazanego sklepu", shopId);
        Long rentalMethodId = queryLong(SELECT_RENTAL_METHOD_ID, "Brak metody wypożyczenia w słowniku");
        Long rentalId = queryLong(SELECT_NEXT_RENTAL_ID, "Nie udało się ustalić nowego id wypożyczenia");

        LocalDate effectiveStartDate = startDate == null ? LocalDate.now() : startDate;
        int insertedRows = jdbcTemplate.update(
                INSERT_RENTAL,
                rentalId,
                bookId,
                userId,
                employeeId,
                shopId,
                Date.valueOf(effectiveStartDate),
                rentalMethodId
        );

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows = jdbcTemplate.update(DELETE_RENTAL, rentalId);
            existsAfterOperation = false;
        }

        return new BookRentalConditionalCreateResult(
                rentalId,
                shopId,
                bookId,
                userId,
                employeeId,
                rentalMethodId,
                effectiveStartDate,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

    private Long queryLong(String sql, String errorMessage, Object... args) {
        Long value = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private boolean exists(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count != null && count > 0;
    }
}