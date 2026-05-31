package bench.app.service.cql.cassandra;

import bench.app.model.common.BookDeliveryCreateResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CassandraBookDeliveryCreateService {
    private static final String INSERT_BOOK = """
            INSERT INTO books_by_shop (
                shop_id, book_id, author, title, publisher,
                publish_date, pages, is_in_reading_room
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String DELETE_BOOK = """
            DELETE FROM books_by_shop
            WHERE shop_id = ? AND book_id = ?
            """;

    private final CqlSession cassandraSession;

    public CassandraBookDeliveryCreateService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookDeliveryCreateResult createDelivery(UUID shopId, int batchSize, boolean restoreAfterCreate) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize musi być większe od 0");
        }
        if (!shopExists(shopId)) {
            throw new IllegalArgumentException("Sklep o podanym shopId nie istnieje");
        }

        int insertedBooks = 0;
        int insertedOfferings = 0;
        List<UUID> createdBookIds = new ArrayList<>(batchSize);

        for (int i = 0; i < batchSize; i++) {
            UUID bookId = UUID.randomUUID();
            insertedBooks += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_BOOK)
                            .addPositionalValues(
                                    shopId,
                                    bookId,
                                    "Autor C6",
                                    "C6_Dostawa_Ksiazka_" + (i + 1),
                                    "Wydawnictwo C6",
                                    LocalDate.of(2020, 1, 1).plusDays(i),
                                    200 + i,
                                    false
                            )
                            .build()
            ).wasApplied() ? 1 : 0;
            insertedOfferings += 1;
            createdBookIds.add(bookId);
        }

        int deletedBooks = 0;
        int deletedOfferings = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            for (UUID bookId : createdBookIds) {
                deletedBooks += cassandraSession.execute(
                        SimpleStatement.builder(DELETE_BOOK)
                                .addPositionalValues(shopId, bookId)
                                .build()
                ).wasApplied() ? 1 : 0;
                deletedOfferings += 1;
            }
            existsAfterOperation = false;
        }

        return new BookDeliveryCreateResult(
                uuidToPositiveLong(shopId),
                batchSize,
                restoreAfterCreate,
                existsAfterOperation,
                insertedBooks,
                insertedOfferings,
                deletedBooks,
                deletedOfferings
        );
    }

    private boolean shopExists(UUID shopId) {
        Row row = cassandraSession.execute(
                        SimpleStatement.builder("SELECT shop_id FROM bookshops WHERE shop_id = ?")
                                .addPositionalValue(shopId)
                                .build())
                .one();
        return row != null;
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }
}