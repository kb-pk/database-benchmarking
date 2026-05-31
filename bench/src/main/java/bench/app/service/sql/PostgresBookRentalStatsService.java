package bench.app.service.sql;

import bench.app.model.common.EmployeeRentalCount;
import bench.app.repository.sql.postgres.PostgresBookRentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostgresBookRentalStatsService {
    private final PostgresBookRentalRepository bookRentalRepository;

    public PostgresBookRentalStatsService(PostgresBookRentalRepository bookRentalRepository) {
        this.bookRentalRepository = bookRentalRepository;
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<EmployeeRentalCount> getEmployeeRentalCountsByShop(long shopId) {
        return bookRentalRepository.findEmployeeRentalCountsByShop((int) shopId);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<EmployeeRentalCount> getEmployeeRentalCountsGlobal() {
        return bookRentalRepository.findEmployeeRentalCountsGlobal();
    }
}
