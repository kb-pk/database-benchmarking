package bench.app.service.cql.cassandra;

import bench.app.model.common.BookRentalDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraBookRentalDeleteService {
    private static final String SELECT_RENTAL_BY_ID = """
            SELECT user_id, start_date, rental_id, book_id, book_title, shop_id,
                   employee_id, is_returned, end_date, rental_method
            FROM rentals_by_user
            WHERE rental_id = ?
            LIMIT 1 ALLOW FILTERING
            """;

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
    private final Map<UUID, RentalSnapshot> snapshots = new ConcurrentHashMap<>();

    public CassandraBookRentalDeleteService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookRentalDeleteResult deleteRental(UUID rentalId, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(rentalId);
        }

        RentalSnapshot snapshot = findRentalSnapshot(rentalId);

        int deletedRows = 0;
        deletedRows += cassandraSession.execute(
                SimpleStatement.builder(DELETE_RENTAL_BY_USER)
                        .addPositionalValues(snapshot.userId(), snapshot.startDate(), snapshot.rentalId())
                        .build()
        ).wasApplied() ? 1 : 0;
        deletedRows += cassandraSession.execute(
                SimpleStatement.builder(DELETE_RENTAL_BY_SHOP)
                        .addPositionalValues(snapshot.shopId(), snapshot.startDate(), snapshot.rentalId())
                        .build()
        ).wasApplied() ? 1 : 0;

        snapshots.put(rentalId, snapshot);

        return new BookRentalDeleteResult(
                uuidToPositiveLong(snapshot.rentalId()),
                uuidToPositiveLong(snapshot.bookId()),
                uuidToPositiveLong(snapshot.userId()),
                uuidToPositiveLong(snapshot.employeeId()),
                uuidToPositiveLong(snapshot.shopId()),
                1L,
                snapshot.isReturned(),
                snapshot.startDate(),
                snapshot.endDate(),
                false,
                false,
                deletedRows,
                0
        );
    }

    private BookRentalDeleteResult restoreFromSnapshot(UUID rentalId) {
        RentalSnapshot snapshot = snapshots.get(rentalId);
        if (snapshot == null) {
            throw new IllegalStateException("Brak snapshotu wypożyczenia o id=" + rentalId);
        }

        int insertedRows = 0;
        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_RENTAL_BY_USER)
                        .addPositionalValues(
                                snapshot.userId(),
                                snapshot.startDate(),
                                snapshot.rentalId(),
                                snapshot.bookId(),
                                snapshot.bookTitle(),
                                snapshot.shopId(),
                                snapshot.employeeId(),
                                snapshot.isReturned(),
                                snapshot.endDate(),
                                snapshot.rentalMethod()
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_RENTAL_BY_SHOP)
                        .addPositionalValues(
                                snapshot.shopId(),
                                snapshot.startDate(),
                                snapshot.rentalId(),
                                snapshot.bookId(),
                                snapshot.userId(),
                                snapshot.employeeId(),
                                snapshot.isReturned(),
                                snapshot.endDate(),
                                snapshot.rentalMethod()
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        snapshots.remove(rentalId);

        return new BookRentalDeleteResult(
                uuidToPositiveLong(snapshot.rentalId()),
                uuidToPositiveLong(snapshot.bookId()),
                uuidToPositiveLong(snapshot.userId()),
                uuidToPositiveLong(snapshot.employeeId()),
                uuidToPositiveLong(snapshot.shopId()),
                1L,
                snapshot.isReturned(),
                snapshot.startDate(),
                snapshot.endDate(),
                true,
                true,
                0,
                insertedRows
        );
    }

    private RentalSnapshot findRentalSnapshot(UUID rentalId) {
        Row row = cassandraSession.execute(
                SimpleStatement.builder(SELECT_RENTAL_BY_ID)
                        .addPositionalValue(rentalId)
                        .build()
        ).one();

        if (row == null) {
            throw new IllegalArgumentException("Nie znaleziono wypożyczenia o id=" + rentalId);
        }

        return new RentalSnapshot(
                row.getUuid("user_id"),
                row.getLocalDate("start_date"),
                row.getUuid("rental_id"),
                row.getUuid("book_id"),
                row.getString("book_title"),
                row.getUuid("shop_id"),
                row.getUuid("employee_id"),
                Boolean.TRUE.equals(row.getBoolean("is_returned")),
                row.getLocalDate("end_date"),
                row.getString("rental_method") == null ? "STANDARD" : row.getString("rental_method")
        );
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }

    private record RentalSnapshot(
            UUID userId,
            LocalDate startDate,
            UUID rentalId,
            UUID bookId,
            String bookTitle,
            UUID shopId,
            UUID employeeId,
            boolean isReturned,
            LocalDate endDate,
            String rentalMethod
    ) {
    }
}
