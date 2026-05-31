package bench.app.service.sql;

import bench.app.benchmark.BookShopOfferingSnapshot;
import bench.app.benchmark.BookShopOfferingSnapshotStore;
import bench.app.model.common.BookShopOfferingDeleteByUserResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;

@Service
public class MssqlBookShopOfferingDeleteService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_MATCHING_OFFERINGS = """
            SELECT o.id AS offering_id, o.bookId AS book_id, o.bookShopId AS book_shop_id
            FROM bench.BookShopOffering o
            WHERE EXISTS (
                SELECT 1
                FROM bench.BookRental br
                WHERE br.userId = ?
                  AND ISNULL(br.isReturned, 0) = 0
                  AND br.bookId = o.bookId
                  AND br.bookShopId = o.bookShopId
            )
            ORDER BY o.id
            """;

    private static final String DELETE_OFFERING_BY_ID = """
            DELETE FROM bench.BookShopOffering
            WHERE id = ?
            """;

    private static final String INSERT_OFFERING = """
            INSERT INTO bench.BookShopOffering (id, bookId, bookShopId)
            VALUES (?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BookShopOfferingSnapshotStore snapshotStore;

    public MssqlBookShopOfferingDeleteService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            BookShopOfferingSnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
        public BookShopOfferingDeleteByUserResult deleteOfferingsForUserPermanentlyBorrowedBooks(
            long userId,
            boolean restoreAfterDelete
    ) {
        if (restoreAfterDelete) {
                        return restoreFromSnapshot(userId);
        }

        List<BookShopOfferingSnapshot> matchedRows = jdbcTemplate.query(
                SELECT_MATCHING_OFFERINGS,
                (rs, rowNum) -> new BookShopOfferingSnapshot(
                        rs.getLong("offering_id"),
                        rs.getLong("book_id"),
                        rs.getLong("book_shop_id")
                ),
                                userId
        );

        int deletedOfferings = 0;
        for (BookShopOfferingSnapshot row : matchedRows) {
            deletedOfferings += jdbcTemplate.update(DELETE_OFFERING_BY_ID, row.offeringId());
        }

        snapshotStore.save(DB_ENGINE, userId, matchedRows);

        return new BookShopOfferingDeleteByUserResult(
                userId,
                matchedRows.size(),
                deletedOfferings,
                0,
                false
        );
    }

    private BookShopOfferingDeleteByUserResult restoreFromSnapshot(long userId) {
        List<BookShopOfferingSnapshot> snapshotRows = snapshotStore.find(DB_ENGINE, userId)
                .orElseThrow(() -> new IllegalStateException("Brak snapshotu D5 dla userId=" + userId));

        int restoredOfferings = 0;
        for (BookShopOfferingSnapshot row : snapshotRows) {
            restoredOfferings += jdbcTemplate.update(
                    INSERT_OFFERING,
                    row.offeringId(),
                    row.bookId(),
                    row.bookShopId()
            );
        }

        snapshotStore.remove(DB_ENGINE, userId);

        return new BookShopOfferingDeleteByUserResult(
                userId,
                snapshotRows.size(),
                0,
                restoredOfferings,
                true
        );
    }
}