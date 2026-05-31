package bench.app.service.cql.scylla;

import bench.app.model.common.EmployeeRentalDayDeleteResult;
import bench.app.service.cql.cassandra.CassandraEmployeeRentalDayDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ScyllaEmployeeRentalDayDeleteService {
    private final CassandraEmployeeRentalDayDeleteService delegate;

    public ScyllaEmployeeRentalDayDeleteService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraEmployeeRentalDayDeleteService(scyllaSession);
    }

    public EmployeeRentalDayDeleteResult deleteRentalsByEmployeeAndDay(UUID employeeId, LocalDate rentalDate, boolean restoreAfterDelete) {
        return this.delegate.deleteRentalsByEmployeeAndDay(employeeId, rentalDate, restoreAfterDelete);
    }
}
