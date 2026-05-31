package bench.app.service.cql.cassandra;

import bench.app.model.common.BookRentalConditionalCreateResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CassandraBookRentalConditionalCreateService {
    private static final String INSERT_RENTAL_BY_USER = """
            INSERT INTO rentals_by_user (
                user_id, start_date, rental_id, book_id, book_title, shop_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL_BY_SHOP = """
            INSERT INTO rentals_by_shop (
                shop_id, start_date, rental_id, book_id, user_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String DELETE_RENTAL_BY_USER = """
            DELETE FROM rentals_by_user
            WHERE user_id = ? AND start_date = ? AND rental_id = ?
            """;

    private static final String DELETE_RENTAL_BY_SHOP = """
            DELETE FROM rentals_by_shop
            WHERE shop_id = ? AND start_date = ? AND rental_id = ?
            """;

    private final CqlSession cassandraSession;

    public CassandraBookRentalConditionalCreateService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookRentalConditionalCreateResult createRentalIfValid(
            UUID shopId,
            UUID bookId,
            UUID userId,
            LocalDate startDate,
            boolean restoreAfterCreate
    ) {
        Row userRow = cassandraSession.execute(
                        SimpleStatement.builder("SELECT status FROM users WHERE user_id = ?")
                                .addPositionalValue(userId)
                                .build())
                .one();
        if (userRow == null || userRow.getString("status") == null || !"ACTIVE".equalsIgnoreCase(userRow.getString("status").trim())) {
            throw new IllegalArgumentException("Użytkownik nie jest aktywny albo nie istnieje");
        }

        Row bookRow = cassandraSession.execute(
                        SimpleStatement.builder("SELECT title FROM books_by_shop WHERE shop_id = ? AND book_id = ?")
                                .addPositionalValues(shopId, bookId)
                                .build())
                .one();
        if (bookRow == null) {
            throw new IllegalArgumentException("Książka nie należy do wskazanego sklepu");
        }

        Row employeeRow = cassandraSession.execute(
                        SimpleStatement.builder("SELECT employee_id FROM employees_by_shop WHERE primary_book_shop_id = ? LIMIT 1")
                                .addPositionalValue(shopId)
                                .build())
                .one();
        if (employeeRow == null) {
            throw new IllegalArgumentException("Brak pracownika przypisanego do sklepu");
        }

        UUID employeeId = employeeRow.getUuid("employee_id");
        UUID rentalId = UUID.randomUUID();
        LocalDate effectiveStartDate = startDate == null ? LocalDate.now() : startDate;
        String bookTitle = bookRow.getString("title");

        int insertedRows = 0;
        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_RENTAL_BY_USER)
                        .addPositionalValues(
                                userId,
                                effectiveStartDate,
                                rentalId,
                                bookId,
                                bookTitle,
                                shopId,
                                employeeId,
                                false,
                                null,
                                "STANDARD"
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_RENTAL_BY_SHOP)
                        .addPositionalValues(
                                shopId,
                                effectiveStartDate,
                                rentalId,
                                bookId,
                                userId,
                                employeeId,
                                false,
                                null,
                                "STANDARD"
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RENTAL_BY_USER)
                            .addPositionalValues(userId, effectiveStartDate, rentalId)
                            .build()
            ).wasApplied() ? 1 : 0;
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RENTAL_BY_SHOP)
                            .addPositionalValues(shopId, effectiveStartDate, rentalId)
                            .build()
            ).wasApplied() ? 1 : 0;
            existsAfterOperation = false;
        }

        return new BookRentalConditionalCreateResult(
                uuidToPositiveLong(rentalId),
                uuidToPositiveLong(shopId),
                uuidToPositiveLong(bookId),
                uuidToPositiveLong(userId),
                uuidToPositiveLong(employeeId),
                1L,
                effectiveStartDate,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }
}