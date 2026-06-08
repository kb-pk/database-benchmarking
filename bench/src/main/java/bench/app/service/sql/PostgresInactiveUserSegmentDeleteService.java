package bench.app.service.sql;

import bench.app.benchmark.InactiveUserSegmentSnapshot;
import bench.app.benchmark.InactiveUserSegmentSnapshotStore;
import bench.app.benchmark.RequestTimingContextHolder;
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
public class PostgresInactiveUserSegmentDeleteService {
    private static final String DB_ENGINE = "POSTGRESQL";

    private static final String SELECT_INACTIVE_CANDIDATE_IDS = """
            SELECT u.id
            FROM bench.bookshopuser u
            JOIN bench.activationstatus a ON a.id = u.isactiveid
            WHERE UPPER(a.status) = 'INACTIVE'
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookreservation r
                  WHERE r.userid = u.id
                    AND r.whenreserved >= (CURRENT_DATE - (? * INTERVAL '1 month'))
              )
              AND NOT EXISTS (
                  SELECT 1
                  FROM bench.bookrental br
                  WHERE br.userid = u.id
                    AND (
                        br.startdate >= (CURRENT_DATE - (? * INTERVAL '1 month'))
                        OR COALESCE(br.enddate, br.startdate) >= (CURRENT_DATE - (? * INTERVAL '1 month'))
                    )
              )
            ORDER BY u.id
            LIMIT ?
            """;

    private static final String SELECT_USER_BY_ID = """
            SELECT id, name, surname, phonenumber, email, mainbookshopid, isactiveid
            FROM bench.bookshopuser
            WHERE id = ?
            """;

    private static final String SELECT_CARDS_BY_USER_ID = """
            SELECT id, cardidnumber, userid, isactiveid
            FROM bench.usercard
            WHERE userid = ?
            ORDER BY id
            """;

    private static final String SELECT_ACCOUNTS_BY_USER_ID = """
            SELECT id, login, passwordhash, userid, permissionsid
            FROM bench.useraccount
            WHERE userid = ?
            ORDER BY id
            """;

    private static final String SELECT_RESERVATIONS_BY_USER_ID = """
            SELECT id, bookid, userid, whenreserved
            FROM bench.bookreservation
            WHERE userid = ?
            ORDER BY id
            """;

    private static final String SELECT_RENTALS_BY_USER_ID = """
            SELECT id, bookid, userid, employeeid, bookshopid, isreturned, startdate, enddate, rentalmethodid
            FROM bench.bookrental
            WHERE userid = ?
            ORDER BY id
            """;

    private static final String DELETE_USER_CARDS = """
            DELETE FROM bench.usercard
            WHERE userid = ?
            """;

    private static final String DELETE_USER_ACCOUNTS = """
            DELETE FROM bench.useraccount
            WHERE userid = ?
            """;

    private static final String DELETE_USER_RESERVATIONS = """
            DELETE FROM bench.bookreservation
            WHERE userid = ?
            """;

    private static final String DELETE_USER_RENTALS = """
            DELETE FROM bench.bookrental
            WHERE userid = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM bench.bookshopuser
            WHERE id = ?
            """;

    private static final String INSERT_USER = """
            INSERT INTO bench.bookshopuser (id, name, surname, phonenumber, email, mainbookshopid, isactiveid)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_USER_CARD = """
            INSERT INTO bench.usercard (id, cardidnumber, userid, isactiveid)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_USER_ACCOUNT = """
            INSERT INTO bench.useraccount (id, login, passwordhash, userid, permissionsid)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String INSERT_RESERVATION = """
            INSERT INTO bench.bookreservation (id, bookid, userid, whenreserved)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_RENTAL = """
            INSERT INTO bench.bookrental (id, bookid, userid, employeeid, bookshopid, isreturned, startdate, enddate, rentalmethodid)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbcTemplate;
    private final InactiveUserSegmentSnapshotStore snapshotStore;
        private final RequestTimingContextHolder timingContextHolder;

    public PostgresInactiveUserSegmentDeleteService(
            @Qualifier("postgresDataSource") DataSource dataSource,
                        InactiveUserSegmentSnapshotStore snapshotStore,
                        RequestTimingContextHolder timingContextHolder
    ) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.snapshotStore = snapshotStore;
                this.timingContextHolder = timingContextHolder;
    }

    @Transactional(transactionManager = "postgresTransactionManager")
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

        List<Long> snapshotCandidateIds = timingContextHolder.excludeFromTiming(() -> loadCandidateIds(monthsThreshold, segmentSize));
        List<InactiveUserSegmentSnapshot.InactiveUserSnapshot> snapshots = timingContextHolder.excludeFromTiming(
                () -> buildSnapshots(snapshotCandidateIds)
        );

        List<Long> candidateIds = loadCandidateIds(monthsThreshold, segmentSize);
        int deletedUsers = 0;
        int deletedCards = 0;
        int deletedAccounts = 0;
        int deletedReservations = 0;
        int deletedRentals = 0;

                if (!candidateIds.isEmpty()) {
                        deletedCards = deleteByUserIds("DELETE FROM bench.usercard WHERE userid", candidateIds);
                        deletedAccounts = deleteByUserIds("DELETE FROM bench.useraccount WHERE userid", candidateIds);
                        deletedReservations = deleteByUserIds("DELETE FROM bench.bookreservation WHERE userid", candidateIds);
                        deletedRentals = deleteByUserIds("DELETE FROM bench.bookrental WHERE userid", candidateIds);
                        deletedUsers = deleteByUserIds("DELETE FROM bench.bookshopuser WHERE id", candidateIds);
        }

        timingContextHolder.excludeFromTiming(() -> snapshotStore.save(DB_ENGINE, monthsThreshold, segmentSize, new InactiveUserSegmentSnapshot(List.copyOf(snapshots))));

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

        private int deleteByUserIds(String deletePrefix, List<Long> userIds) {
                final int batchSize = 500;
                int deleted = 0;

                for (int offset = 0; offset < userIds.size(); offset += batchSize) {
                        int end = Math.min(offset + batchSize, userIds.size());
                        List<Long> chunk = userIds.subList(offset, end);

                        StringBuilder placeholders = new StringBuilder();
                        for (int i = 0; i < chunk.size(); i++) {
                                if (i > 0) {
                                        placeholders.append(",");
                                }
                                placeholders.append("?");
                        }

                        String sql = deletePrefix + " IN (" + placeholders + ")";
                        deleted += jdbcTemplate.update(sql, chunk.toArray());
                }

                return deleted;
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

    private List<Long> loadCandidateIds(int monthsThreshold, int segmentSize) {
        return jdbcTemplate.query(
                SELECT_INACTIVE_CANDIDATE_IDS,
                (rs, rowNum) -> rs.getLong("id"),
                monthsThreshold,
                monthsThreshold,
                monthsThreshold,
                segmentSize
        );
    }

    private List<InactiveUserSegmentSnapshot.InactiveUserSnapshot> buildSnapshots(List<Long> candidateIds) {
        List<InactiveUserSegmentSnapshot.InactiveUserSnapshot> snapshots = new ArrayList<>();

        for (Long userId : candidateIds) {
            InactiveUserSegmentSnapshot.UserRow userRow = jdbcTemplate.query(
                    SELECT_USER_BY_ID,
                    rs -> rs.next() ? new InactiveUserSegmentSnapshot.UserRow(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("surname"),
                            rs.getString("phonenumber"),
                            rs.getString("email"),
                            rs.getObject("mainbookshopid") == null ? null : rs.getLong("mainbookshopid"),
                            rs.getLong("isactiveid")
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
                            rs.getString("cardidnumber"),
                            rs.getObject("userid") == null ? null : rs.getLong("userid"),
                            rs.getLong("isactiveid")
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.UserAccountRow> accounts = jdbcTemplate.query(
                    SELECT_ACCOUNTS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.UserAccountRow(
                            rs.getLong("id"),
                            rs.getString("login"),
                            rs.getString("passwordhash"),
                            rs.getLong("userid"),
                            rs.getLong("permissionsid")
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.BookReservationRow> reservations = jdbcTemplate.query(
                    SELECT_RESERVATIONS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.BookReservationRow(
                            rs.getLong("id"),
                            rs.getLong("bookid"),
                            rs.getLong("userid"),
                            rs.getDate("whenreserved").toLocalDate()
                    ),
                    userId
            );

            List<InactiveUserSegmentSnapshot.BookRentalRow> rentals = jdbcTemplate.query(
                    SELECT_RENTALS_BY_USER_ID,
                    (rs, rowNum) -> new InactiveUserSegmentSnapshot.BookRentalRow(
                            rs.getLong("id"),
                            rs.getLong("bookid"),
                            rs.getLong("userid"),
                            rs.getLong("employeeid"),
                            rs.getLong("bookshopid"),
                            rs.getInt("isreturned") != 0,
                            rs.getDate("startdate").toLocalDate(),
                            rs.getDate("enddate") == null ? null : rs.getDate("enddate").toLocalDate(),
                            rs.getLong("rentalmethodid")
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
        }

        return snapshots;
    }
}