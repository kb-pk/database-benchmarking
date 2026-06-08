package bench.app.service.sql;

import bench.app.model.common.BookDeliveryCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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
        List<BookInsertRow> booksToInsert = new ArrayList<>(batchSize);
        List<OfferingInsertRow> offeringsToInsert = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; i++) {
            long bookId = startBookId + i;
            long offeringId = startOfferingId + i;
            String title = "C6_Dostawa_Ksiazka_" + (i + 1);

            booksToInsert.add(new BookInsertRow(
                    bookId,
                    "Autor C6",
                    title,
                    "Wydawnictwo C6",
                    Date.valueOf(LocalDate.of(2020, 1, 1).plusDays(i)),
                    200 + i,
                    shopId
            ));
            offeringsToInsert.add(new OfferingInsertRow(offeringId, bookId, shopId));

            createdBookIds.add(bookId);
            createdOfferingIds.add(offeringId);
        }

        insertedBooks = sumBatchResults(jdbcTemplate.batchUpdate(INSERT_BOOK, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                BookInsertRow row = booksToInsert.get(i);
                ps.setLong(1, row.bookId());
                ps.setString(2, row.author());
                ps.setString(3, row.title());
                ps.setString(4, row.publisher());
                ps.setDate(5, row.publishDate());
                ps.setInt(6, row.pages());
                ps.setLong(7, row.shopId());
            }

            @Override
            public int getBatchSize() {
                return booksToInsert.size();
            }
        }));

        insertedOfferings = sumBatchResults(jdbcTemplate.batchUpdate(INSERT_OFFERING, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                OfferingInsertRow row = offeringsToInsert.get(i);
                ps.setLong(1, row.offeringId());
                ps.setLong(2, row.bookId());
                ps.setLong(3, row.shopId());
            }

            @Override
            public int getBatchSize() {
                return offeringsToInsert.size();
            }
        }));

        int deletedBooks = 0;
        int deletedOfferings = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            deletedOfferings = sumBatchResults(jdbcTemplate.batchUpdate(
                    DELETE_OFFERING,
                    createdOfferingIds,
                    Math.min(500, createdOfferingIds.size()),
                    (ps, id) -> ps.setLong(1, id)
            ));
            deletedBooks = sumBatchResults(jdbcTemplate.batchUpdate(
                    DELETE_BOOK,
                    createdBookIds,
                    Math.min(500, createdBookIds.size()),
                    (ps, id) -> ps.setLong(1, id)
            ));
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

    private int sumBatchResults(int[] batchResults) {
        int sum = 0;
        for (int result : batchResults) {
            sum += Math.max(result, 0);
        }
        return sum;
    }

    private int sumBatchResults(int[][] batchResults) {
        int sum = 0;
        for (int[] chunk : batchResults) {
            sum += sumBatchResults(chunk);
        }
        return sum;
    }

    private record BookInsertRow(
            long bookId,
            String author,
            String title,
            String publisher,
            Date publishDate,
            int pages,
            long shopId
    ) {
    }

    private record OfferingInsertRow(
            long offeringId,
            long bookId,
            long shopId
    ) {
    }
}