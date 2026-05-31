package bench.app.service.sql;

import bench.app.benchmark.InactiveUserSegmentSnapshot;
import bench.app.benchmark.InactiveUserSegmentSnapshotStore;
import bench.app.model.common.UserInactiveSegmentDeleteResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

@Service
public class MssqlInactiveUserSegmentDeleteService {
    private static final String DB_ENGINE = "MSSQL";

    private static final String SELECT_INACTIVE_CANDIDATE_IDS = """
            SELECT TOP (?) u.id
            FROM bench.BookShopUser u
            JOIN bench.ActivationStatus a ON a.id = u.isActiveId
            WHERE UPPER(a.status) = 'INACTIVE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookReservation r
                  WHERE r.userId = u.id
                    AND r.whenReserved >= DATEADD(MONTH, -?, CAST(GETDATE() AS date))
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.BookRental br
                  WHERE br.userId = u.id
                    AND (
                        br.startDate >= DATEADD(MONTH, -?, CAST(GETDATE() AS date))
                        OR ISNULL(br.endDate, br.startDate) >= DATEADD(MONTH, -?, CAST(GETDATE() AS date))
                    )
              )
            ORDER BY u.id
            """;

    private static final String SELECT_USER_BY_ID = """
            SELECT id, name, surname, phoneNumber, email, mainBookShopId, isActiveId
            FROM bench.BookShopUser
            WHERE id = ?
            """;

    private static final String SELECT_CARDS_BY_USER_ID = """
            SELECT id, cardIdNumber, userId, isActiveId
            FROM bench.UserCard
            WHERE userId = ?
            ORDER BY id
            """;

    private static final String SELECT_ACCOUNTS_BY_USER_ID = """
            SELECT id, login, passwordHash, userId, permissionsId
            FROM bench.UserAccount
            WHERE userId = ?
            ORDER BY id
            """;

    private static final String SELECT_RESERVATIONS_BY_USER_ID = """
            SELECT id, bookId, userId, whenReserved
            FROM bench.BookReservation
            WHERE userId = ?
            ORDER BY id
            """;

    private static final String SELECT_RENTALS_BY_USER_ID = """
            SELECT id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId
            FROM bench.BookRental
            WHERE userId = ?
            ORDER BY id
            """;

    private static final String DELETE_USER_CARDS = """
            DELETE FROM bench.UserCard
            WHERE userId = ?
            """;

    private static final String DELETE_USER_ACCOUNTS = """
            DELETE FROM bench.UserAccount
            WHERE userId = ?
            """;

    private static final String DELETE_USER_RESERVATIONS = """
            DELETE FROM bench.BookReservation
            WHERE userId = ?
            """;

    private static final String DELETE_USER_RENTALS = """
            DELETE FROM bench.BookRental
            WHERE userId = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM bench.BookShopUser
            WHERE id = ?
            """;

    private static final String INSERT_USER = """
            INSERT INTO bench.BookShopUser (id, name, surname, phoneNumber, email, mainBookShopId, isActiveId)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_USER_CARD = """
            INSERT INTO bench.UserCard (id, cardIdNumber, userId, isActiveId)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_USER_ACCOUNT = """
            INSERT INTO bench.UserAccount (id, login, passwordHash, userId, permissionsId)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO bench.BookReservation (id, bookId, userId, whenReserved)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL = """
            INSERT INTO bench.BookRental (id, bookId, userId, employeeId, bookShopId, isReturned, startDate, endDate, rentalMethodId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final InactiveUserSegmentSnapshotStore snapshotStore;

    public MssqlInactiveUserSegmentDeleteService(
            @Qualifier("mssqlDataSource") DataSource dataSource,
            InactiveUserSegmentSnapshotStore snapshotStore
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
    public UserInactiveSegmentDeleteResult deleteInactiveUsersWithoutRecentActivity(
            int monthsThreshold,
            int segmentSize,
            boolean restoreAfterDelete
    ) {
        if (monthsThreshold <= 0) {
            throw new IllegalArgumentException("monthsThreshold musi być większe od 0");
        }
        if (segmentSize <= 0) {
            throw new IllegalArgumentException("segmentSize musi być większe od 0");
        }

        if (restoreAfterDelete) {
            return restoreFromSnapshot(monthsThreshold, segmentSize);
        }

        List<Long> candidateIds = jdbcTemplate.query(
                SELECT_INACTIVE_CANDIDATE_IDS,
                (rs, rowNum) -> rs.getLong("id"),
                segmentSize,
                monthsThreshold,
                monthsThreshold,
                monthsThreshold
        );

        List<InactiveUserSegmentSnapshot.InactiveUserSnapshot> snapshots = new ArrayList<>();
        int deletedUsers = 0;
        int deletedCards = 0;
        int deletedAccounts = 0;
        int deletedReservations = 0;
        int deletedRentals = 0;

        for (Long userId : candidateIds) {
            InactiveUserSegmentSnapshot.UserRow userRow = jdbcTemplate.query(
                    SELECT_USER_BY_ID,
                    rs -> rs.next() ? new InactiveUserSegmentSnapshot.UserRow(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("phoneNumber"),
                            rs.getString("email"),
                            rs.getObject("mainBookShopId") == null ? null : rs.getLong("mainBookShopId"),
                            rs.getLong("isActiveId")
                    ) : null,
                    userId
            );

            if (userRow == null) {
                continue;
            }

            List<InactiveUserSegmentSnapshot.UserCardRow> cards = jdbcTemplate.query(
                    SELECT_CARDS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.UserCardRow(
                            rs.getLong("id"),
                            rs.getString("cardIdNumber"),
                            rs.getObject("userId") == null ? null : rs.getLong("userId"),
                            rs.getLong("isActiveId")
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.UserAccountRow> accounts = jdbcTemplate.query(
                    SELECT_ACCOUNTS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.UserAccountRow(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("passwordHash"),
                            rs.getLong("userId"),
                            rs.getLong("permissionsId")
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.BookReservationRow> reservations = jdbcTemplate.query(
                    SELECT_RESERVATIONS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.BookReservationRow(
                            rs.getLong("id"),
                            rs.getLong("bookId"),
                            rs.getLong("userId"),
                            rs.getDate("whenReserved").toLocalDate()
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.BookRentalRow> rentals = jdbcTemplate.query(
                    SELECT_RENTALS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.BookRentalRow(
                            rs.getLong("id"),
                            rs.getLong("bookId"),
                            rs.getLong("userId"),
                            rs.getLong("employeeId"),
                            rs.getLong("bookShopId"),
                            rs.getInt("isReturned") != 0,
                            rs.getDate("startDate").toLocalDate(),
                            rs.getDate("endDate") == null ? null : rs.getDate("endDate").toLocalDate(),
                            rs.getLong("rentalMethodId")
                    ),
                    userId
            );

            snapshots.add(new InactiveUserSegmentSnapshot.InactiveUserSnapshot(
                    userRow,
                    cards,
                    accounts,
                    reservations,
                    rentals
            ));

            deletedCards += jdbcTemplate.update(DELETE_USER_CARDS, userId);
            deletedAccounts += jdbcTemplate.update(DELETE_USER_ACCOUNTS, userId);
            deletedReservations += jdbcTemplate.update(DELETE_USER_RESERVATIONS, userId);
            deletedRentals += jdbcTemplate.update(DELETE_USER_RENTALS, userId);
            deletedUsers += jdbcTemplate.update(DELETE_USER, userId);
        }

        snapshotStore.save(DB_ENGINE, monthsThreshold, segmentSize, new InactiveUserSegmentSnapshot(List.copyOf(snapshots)));

        return new UserInactiveSegmentDeleteResult(
                monthsThreshold,
                segmentSize,
                candidateIds.size(),
                deletedUsers,
                deletedCards,
                deletedAccounts,
                deletedReservations,
                deletedRentals,
                0,
                0,
                0,
                0,
                0,
                false
        );
    }

    private UserInactiveSegmentDeleteResult restoreFromSnapshot(int monthsThreshold, int segmentSize) {
        InactiveUserSegmentSnapshot snapshot = snapshotStore.find(DB_ENGINE, monthsThreshold, segmentSize)
                .orElseThrow(() -> new IllegalStateException("Brak snapshotu D4 dla monthsThreshold=" + monthsThreshold + " segmentSize=" + segmentSize));

        int restoredUsers = 0;
        int restoredCards = 0;
        int restoredAccounts = 0;
        int restoredReservations = 0;
        int restoredRentals = 0;

        for (InactiveUserSegmentSnapshot.InactiveUserSnapshot userSnapshot : snapshot.users()) {
            InactiveUserSegmentSnapshot.UserRow user = userSnapshot.user();
            restoredUsers += jdbcTemplate.update(
                    INSERT_USER,
                    user.id(),
                    user.name(),
                    user.surname(),
                    user.phoneNumber(),
                    user.email(),
                    user.mainBookShopId(),
                    user.isActiveId()
            );

            for (InactiveUserSegmentSnapshot.UserCardRow card : userSnapshot.cards()) {
                restoredCards += jdbcTemplate.update(
                        INSERT_USER_CARD,
                        card.id(),
                        card.cardIdNumber(),
                        card.userId(),
                        card.isActiveId()
                );
            }

            for (InactiveUserSegmentSnapshot.UserAccountRow account : userSnapshot.accounts()) {
                restoredAccounts += jdbcTemplate.update(
                        INSERT_USER_ACCOUNT,
                        account.id(),
                        account.login(),
                        account.passwordHash(),
                        account.userId(),
                        account.permissionsId()
                );
            }

            for (InactiveUserSegmentSnapshot.BookReservationRow reservation : userSnapshot.reservations()) {
                restoredReservations += jdbcTemplate.update(
                        INSERT_RESERVATION,
                        reservation.id(),
                        reservation.bookId(),
                        reservation.userId(),
                        Date.valueOf(reservation.whenReserved())
                );
            }

            for (InactiveUserSegmentSnapshot.BookRentalRow rental : userSnapshot.rentals()) {
                restoredRentals += jdbcTemplate.update(
                        INSERT_RENTAL,
                        rental.id(),
                        rental.bookId(),
                        rental.userId(),
                        rental.employeeId(),
                        rental.bookShopId(),
                        rental.isReturned() ? 1 : 0,
                        Date.valueOf(rental.startDate()),
                        rental.endDate() == null ? null : Date.valueOf(rental.endDate()),
                        rental.rentalMethodId()
                );
            }
        }

        snapshotStore.remove(DB_ENGINE, monthsThreshold, segmentSize);

        return new UserInactiveSegmentDeleteResult(
                monthsThreshold,
                segmentSize,
                snapshot.users().size(),
                0,
                0,
                0,
                0,
                0,
                restoredUsers,
                restoredCards,
                restoredAccounts,
                restoredReservations,
                restoredRentals,
                true
        );
    }
}