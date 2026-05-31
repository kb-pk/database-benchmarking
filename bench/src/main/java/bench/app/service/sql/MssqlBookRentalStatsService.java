package bench.app.service.sql;

import bench.app.model.common.EmployeeRentalCount;
import bench.app.repository.sql.mssql.MssqlBookRentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MssqlBookRentalStatsService {
    private final MssqlBookRentalRepository bookRentalRepository;

    public MssqlBookRentalStatsService(MssqlBookRentalRepository bookRentalRepository) {
        this.bookRentalRepository = bookRentalRepository;
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<EmployeeRentalCount> getEmployeeRentalCountsByShop(long shopId) {
        return bookRentalRepository.findEmployeeRentalCountsByShop((int) shopId);
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<EmployeeRentalCount> getEmployeeRentalCountsGlobal() {
        return bookRentalRepository.findEmployeeRentalCountsGlobal();
    }
}
