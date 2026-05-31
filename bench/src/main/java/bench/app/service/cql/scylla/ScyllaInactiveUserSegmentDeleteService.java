package bench.app.service.cql.scylla;

import bench.app.model.common.UserInactiveSegmentDeleteResult;
import bench.app.service.cql.cassandra.CassandraInactiveUserSegmentDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ScyllaInactiveUserSegmentDeleteService {
    private final CassandraInactiveUserSegmentDeleteService delegate;

    public ScyllaInactiveUserSegmentDeleteService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraInactiveUserSegmentDeleteService(scyllaSession);
    }

    public UserInactiveSegmentDeleteResult deleteInactiveUsersWithoutRecentActivity(int monthsThreshold, int segmentSize, boolean restoreAfterDelete) {
        return this.delegate.deleteInactiveUsersWithoutRecentActivity(monthsThreshold, segmentSize, restoreAfterDelete);
    }
}
