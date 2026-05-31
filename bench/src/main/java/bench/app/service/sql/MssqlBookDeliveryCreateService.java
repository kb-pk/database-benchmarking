package bench.app.service.sql;

import bench.app.model.common.BookDeliveryCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MssqlBookDeliveryCreateService {
    private static final String CHECK_SHOP_EXISTS = """
            SELECT COUNT(*)
            FROM bench.BookShop
            WHERE id = ?
            """;

    private static final String SELECT_NEXT_BOOK_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.Book
            """;

    private static final String SELECT_NEXT_OFFERING_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.BookShopOffering
            """;

    private static final String INSERT_BOOK = """
            INSERT INTO bench.Book (id, author, title, publisher, publishDate, pages, isInReadingRoom, bookShopId)
            VALUES (?, ?, ?, ?, ?, ?, 0, ?)
            """;

    private static final String INSERT_OFFERING = """
            INSERT INTO bench.BookShopOffering (id, bookId, bookShopId)
            VALUES (?, ?, ?)
            """;

    private static final String DELETE_OFFERING = """
            DELETE FROM bench.BookShopOffering
            WHERE id = ?
            """;

    private static final String DELETE_BOOK = """
            DELETE FROM bench.Book
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookDeliveryCreateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public BookDeliveryCreateResult createDelivery(long shopId, int batchSize, boolean restoreAfterCreate) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize musi być większe od 0");
        }
        if (!existsShop(shopId)) {
            throw new IllegalArgumentException("Sklep o podanym shopId nie istnieje");
        }

        Long startBookId = queryLong(SELECT_NEXT_BOOK_ID, "Nie udało się ustalić startowego id książki");
        Long startOfferingId = queryLong(SELECT_NEXT_OFFERING_ID, "Nie udało się ustalić startowego id oferty");

        int insertedBooks = 0;
        int insertedOfferings = 0;
        List<Long> createdBookIds = new ArrayList<>(batchSize);
        List<Long> createdOfferingIds = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; i++) {
            long bookId = startBookId + i;
            long offeringId = startOfferingId + i;
            String title = "C6_Dostawa_Ksiazka_" + (i + 1);

            insertedBooks += jdbcTemplate.update(
                    INSERT_BOOK,
                    bookId,
                    "Autor C6",
                    title,
                    "Wydawnictwo C6",
                    Date.valueOf(LocalDate.of(2020, 1, 1).plusDays(i)),
                    200 + i,
                    shopId
            );
            insertedOfferings += jdbcTemplate.update(INSERT_OFFERING, offeringId, bookId, shopId);

            createdBookIds.add(bookId);
            createdOfferingIds.add(offeringId);
        }

        int deletedBooks = 0;
        int deletedOfferings = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            for (Long offeringId : createdOfferingIds) {
                deletedOfferings += jdbcTemplate.update(DELETE_OFFERING, offeringId);
            }
            for (Long bookId : createdBookIds) {
                deletedBooks += jdbcTemplate.update(DELETE_BOOK, bookId);
            }
            existsAfterOperation = false;
        }

        return new BookDeliveryCreateResult(
                shopId,
                batchSize,
                restoreAfterCreate,
                existsAfterOperation,
                insertedBooks,
                insertedOfferings,
                deletedBooks,
                deletedOfferings
        );
    }

    private Long queryLong(String sql, String errorMessage) {
        Long value = jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private boolean existsShop(long shopId) {
        Integer count = jdbcTemplate.queryForObject(CHECK_SHOP_EXISTS, Integer.class, shopId);
        return count != null && count > 0;
    }
}