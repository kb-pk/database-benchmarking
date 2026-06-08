package bench.app.service.cql.scylla;

import bench.app.benchmark.RequestTimingContextHolder;
import bench.app.model.common.BookReservationBulkDeleteResult;
import bench.app.service.cql.cassandra.CassandraBookReservationBulkDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ScyllaBookReservationBulkDeleteService {
    private final CassandraBookReservationBulkDeleteService delegate;

    public ScyllaBookReservationBulkDeleteService(
            @Qualifier("scyllaSession") CqlSession scyllaSession,
            RequestTimingContextHolder timingContextHolder
    ) {
        this.delegate = new CassandraBookReservationBulkDeleteService(scyllaSession, timingContextHolder);
    }

    public BookReservationBulkDeleteResult deleteOldUnfinalizedReservations(int monthsThreshold, boolean restoreAfterDelete) {
        return this.delegate.deleteOldUnfinalizedReservations(monthsThreshold, restoreAfterDelete);
    }
}
