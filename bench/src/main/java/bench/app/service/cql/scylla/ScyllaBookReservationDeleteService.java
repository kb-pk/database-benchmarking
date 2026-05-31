package bench.app.service.cql.scylla;

import bench.app.model.common.BookReservationDeleteResult;
import bench.app.service.cql.cassandra.CassandraBookReservationDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScyllaBookReservationDeleteService {
    private final CassandraBookReservationDeleteService delegate;

    public ScyllaBookReservationDeleteService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookReservationDeleteService(scyllaSession);
    }

    public BookReservationDeleteResult deleteReservation(UUID reservationId, boolean restoreAfterDelete) {
        return this.delegate.deleteReservation(reservationId, restoreAfterDelete);
    }
}
