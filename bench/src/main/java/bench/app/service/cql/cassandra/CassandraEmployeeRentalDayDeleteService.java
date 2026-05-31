package bench.app.service.cql.cassandra;

import bench.app.model.common.EmployeeRentalDayDeleteResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CassandraEmployeeRentalDayDeleteService {
    private static final String SELECT_RENTALS_BY_EMPLOYEE_AND_DAY = """
            SELECT shop_id, start_date, rental_id, book_id, user_id, employee_id, is_returned, end_date, rental_method
            FROM rentals_by_shop
            WHERE employee_id = ? AND start_date = ? ALLOW FILTERING
            """;

    private static final String SELECT_BOOK_TITLE = """
            SELECT title
            FROM books_by_shop
            WHERE shop_id = ? AND book_id = ?
            """;

    private static final String DELETE_RENTAL_BY_SHOP = """
            DELETE FROM rentals_by_shop
            WHERE shop_id = ? AND start_date = ? AND rental_id = ?
            """;

    private static final String DELETE_RENTAL_BY_USER = """
            DELETE FROM rentals_by_user
            WHERE user_id = ? AND start_date = ? AND rental_id = ?
            """;

    private static final String INSERT_RENTAL_BY_SHOP = """
            INSERT INTO rentals_by_shop (
                shop_id, start_date, rental_id, book_id, user_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL_BY_USER = """
            INSERT INTO rentals_by_user (
                user_id, start_date, rental_id, book_id, book_title, shop_id,
                employee_id, is_returned, end_date, rental_method
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final CqlSession cassandraSession;
    private final Map<String, List<RentalSnapshot>> snapshots = new ConcurrentHashMap<>();

    public CassandraEmployeeRentalDayDeleteService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public EmployeeRentalDayDeleteResult deleteRentalsByEmployeeAndDay(UUID employeeId, LocalDate rentalDate, boolean restoreAfterDelete) {
        if (restoreAfterDelete) {
            return restoreFromSnapshot(employeeId, rentalDate);
        }

        ResultSet rows = cassandraSession.execute(
                SimpleStatement.builder(SELECT_RENTALS_BY_EMPLOYEE_AND_DAY)
                        .addPositionalValues(employeeId, rentalDate)
                        .build()
        );

        List<RentalSnapshot> matched = new ArrayList<>();
        for (Row row : rows) {
            UUID shopId = row.getUuid("shop_id");
            UUID bookId = row.getUuid("book_id");
            String bookTitle = readBookTitle(shopId, bookId);

            matched.add(new RentalSnapshot(
                    row.getUuid("shop_id"),
                    row.getLocalDate("start_date"),
                    row.getUuid("rental_id"),
                    row.getUuid("book_id"),
                    row.getUuid("user_id"),
                    row.getUuid("employee_id"),
                    Boolean.TRUE.equals(row.getBoolean("is_returned")),
                    row.getLocalDate("end_date"),
                    row.getString("rental_method") == null ? "STANDARD" : row.getString("rental_method"),
                    bookTitle
            ));
        }

        int deletedRows = 0;
        for (RentalSnapshot snapshot : matched) {
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RENTAL_BY_SHOP)
                            .addPositionalValues(snapshot.shopId(), snapshot.startDate(), snapshot.rentalId())
                            .build()
            ).wasApplied() ? 1 : 0;
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_RENTAL_BY_USER)
                            .addPositionalValues(snapshot.userId(), snapshot.startDate(), snapshot.rentalId())
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshots.put(snapshotKey(employeeId, rentalDate), List.copyOf(matched));

        return new EmployeeRentalDayDeleteResult(
                uuidToPositiveLong(employeeId),
                rentalDate,
                matched.size(),
                deletedRows,
                0,
                false
        );
    }

    private EmployeeRentalDayDeleteResult restoreFromSnapshot(UUID employeeId, LocalDate rentalDate) {
        String key = snapshotKey(employeeId, rentalDate);
        List<RentalSnapshot> snapshotRows = snapshots.get(key);
        if (snapshotRows == null) {
            throw new IllegalStateException("Brak snapshotu D6 dla employeeId=" + employeeId + " rentalDate=" + rentalDate);
        }

        int restoredRows = 0;
        for (RentalSnapshot row : snapshotRows) {
            restoredRows += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_RENTAL_BY_SHOP)
                            .addPositionalValues(
                                    row.shopId(),
                                    row.startDate(),
                                    row.rentalId(),
                                    row.bookId(),
                                    row.userId(),
                                    row.employeeId(),
                                    row.isReturned(),
                                    row.endDate(),
                                    row.rentalMethod()
                            )
                            .build()
            ).wasApplied() ? 1 : 0;

            restoredRows += cassandraSession.execute(
                    SimpleStatement.builder(INSERT_RENTAL_BY_USER)
                            .addPositionalValues(
                                    row.userId(),
                                    row.startDate(),
                                    row.rentalId(),
                                    row.bookId(),
                                    row.bookTitle(),
                                    row.shopId(),
                                    row.employeeId(),
                                    row.isReturned(),
                                    row.endDate(),
                                    row.rentalMethod()
                            )
                            .build()
            ).wasApplied() ? 1 : 0;
        }

        snapshots.remove(key);

        return new EmployeeRentalDayDeleteResult(
                uuidToPositiveLong(employeeId),
                rentalDate,
                snapshotRows.size(),
                0,
                restoredRows,
                true
        );
    }

    private String readBookTitle(UUID shopId, UUID bookId) {
        Row row = cassandraSession.execute(
                SimpleStatement.builder(SELECT_BOOK_TITLE)
                        .addPositionalValues(shopId, bookId)
                        .build()
        ).one();
        return row == null ? null : row.getString("title");
    }

    private String snapshotKey(UUID employeeId, LocalDate rentalDate) {
        return employeeId + ":" + rentalDate;
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }

    private record RentalSnapshot(
            UUID shopId,
            LocalDate startDate,
            UUID rentalId,
            UUID bookId,
            UUID userId,
            UUID employeeId,
            boolean isReturned,
            LocalDate endDate,
            String rentalMethod,
            String bookTitle
    ) {
    }
}
