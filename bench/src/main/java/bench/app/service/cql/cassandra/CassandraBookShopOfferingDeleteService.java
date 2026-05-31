package bench.app.service.cql.cassandra;

import bench.app.model.common.BookShopOfferingDeleteByUserResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraBookShopOfferingDeleteService {
    private static final String SELECT_USER_RENTALS = """
            SELECT book_id, shop_id, is_returned
            FROM rentals_by_user
            WHERE user_id = ?
            """;

    private static final String SELECT_BOOK_BY_SHOP = """
            SELECT author, title, publisher, publish_date, pages, is_in_reading_room
            FROM books_by_shop
            WHERE shop_id = ? AND book_id = ?
            """;

    private static final String DELETE_BOOK_BY_SHOP = """
            DELETE FROM books_by_shop
            WHERE shop_id = ? AND book_id = ?
            """;

    private static final String INSERT_BOOK_BY_SHOP = """
            INSERT INTO books_by_shop (shop_id, book_id, author, title, publisher, publish_date, pages, is_in_reading_room)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final CqlSession cassandraSession;
    private final Map<UUID, List<BookSnapshot>> snapshotsByUser = new ConcurrentHashMap<>();

    public CassandraBookShopOfferingDeleteService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookShopOfferingDeleteByUserResult deleteOfferingsForUserPermanentlyBorrowedBooks(UUID userId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(userId);
        }

        Set<BookKey> permanentlyBorrowed = new LinkedHashSet<>();
        ResultSet rentals = cassandraSession.execute(
                SimpleStatement.builder(SELECT_USER_RENTALS)
                        .addPositionalValue(userId)
                        .build()
        );

        for (Row row : rentals) {
            if (Boolean.TRUE.equals(row.getBoolean("is_returned"))) {
                continue;
            }
            UUID shopId = row.getUuid("shop_id");
            UUID bookId = row.getUuid("book_id");
            permanentlyBorrowed.add(new BookKey(shopId, bookId));
        }

        List<BookSnapshot> matched = new ArrayList<>();
        int deletedRows = 0;

        for (BookKey key : permanentlyBorrowed) {
            Row bookRow = cassandraSession.execute(
                    SimpleStatement.builder(SELECT_BOOK_BY_SHOP)
                            .addPositionalValues(key.shopId(), key.bookId())
                            .build()
            ).one();

            if (bookRow == null) {
                continue;
            }

            matched.add(new BookSnapshot(
                    key.shopId(),
                    key.bookId(),
                    bookRow.getString("author"),
                    bookRow.getString("title"),
                    bookRow.getString("publisher"),
                    bookRow.getLocalDate("publish_date"),
                    bookRow.getInt("pages"),
                    Boolean.TRUE.equals(bookRow.getBoolean("is_in_reading_room"))
            ));

            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_BOOK_BY_SHOP)
                            .addPositionalValues(key.shopId(), key.bookId())
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshotsByUser.put(userId, List.copyOf(matched));

        return new BookShopOfferingDeleteByUserResult(
                uuidToPositiveLong(userId),
                matched.size(),
                deletedRows,
                0,
                false
        );
    }

    private BookShopOfferingDeleteByUserResult restoreFromSnapshot(UUID userId) {
        List<BookSnapshot> snapshots = snapshotsByUser.get(userId);
        if (snapshots == null) {
            throw new IllegalStateException("Brak snapshotu D5 dla userId=" + userId);
        }

        int restoredRows = 0;
        for (BookSnapshot snapshot : snapshots) {
            restoredRows += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_BOOK_BY_SHOP)
                            .addPositionalValues(
                                    snapshot.shopId(),
                                    snapshot.bookId(),
                                    snapshot.author(),
                                    snapshot.title(),
                                    snapshot.publisher(),
                                    snapshot.publishDate(),
                                    snapshot.pages(),
                                    snapshot.isInReadingRoom()
                            )
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshotsByUser.remove(userId);

        return new BookShopOfferingDeleteByUserResult(
                uuidToPositiveLong(userId),
                snapshots.size(),
                0,
                restoredRows,
                true
        );
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }

    private record BookKey(UUID shopId, UUID bookId) {
    }

    private record BookSnapshot(
            UUID shopId,
            UUID bookId,
            String author,
            String title,
            String publisher,
            LocalDate publishDate,
            Integer pages,
            boolean isInReadingRoom
    ) {
    }
}
