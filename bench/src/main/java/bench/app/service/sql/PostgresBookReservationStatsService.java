package bench.app.service.sql;

import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserReservationCount;
import bench.app.repository.sql.postgres.PostgresBookReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
public class PostgresBookReservationStatsService {
    private final PostgresBookReservationRepository bookReservationRepository;

    public PostgresBookReservationStatsService(PostgresBookReservationRepository bookReservationRepository) {
        this.bookReservationRepository = bookReservationRepository;
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<UserReservationCount> getTopUsersByReservationCount(long shopId) {
        return bookReservationRepository.findTopUsersByReservationCount((int) shopId);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<UserReservationCount> getTopUsersByReservationCountGlobal() {
        return bookReservationRepository.findTopUsersByReservationCountGlobal();
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<EngagedUser> getEngagedUsersByShopAndPeriod(long shopId, LocalDate fromDate, LocalDate toDate) {
        return bookReservationRepository.findEngagedUsersByShopAndPeriod(
                (int) shopId,
                Date.valueOf(fromDate),
                Date.valueOf(toDate)
        );
    }
}
