package bench.app.service.cql.scylla;

import bench.app.model.common.BookRentalDeleteResult;
import bench.app.service.cql.cassandra.CassandraBookRentalDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScyllaBookRentalDeleteService {
    private final CassandraBookRentalDeleteService delegate;

    public ScyllaBookRentalDeleteService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookRentalDeleteService(scyllaSession);
    }

    public BookRentalDeleteResult deleteRental(UUID rentalId, boolean restoreAfterDelete) {
        return this.delegate.deleteRental(rentalId, restoreAfterDelete);
    }
}
