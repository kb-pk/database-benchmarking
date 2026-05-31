package bench.app.service.cql.scylla;

import bench.app.model.common.BookDeliveryCreateResult;
import bench.app.service.cql.cassandra.CassandraBookDeliveryCreateService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScyllaBookDeliveryCreateService {
    private final CassandraBookDeliveryCreateService delegate;

    public ScyllaBookDeliveryCreateService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookDeliveryCreateService(scyllaSession);
    }

    public BookDeliveryCreateResult createDelivery(UUID shopId, int batchSize, boolean restoreAfterCreate) {
        return this.delegate.createDelivery(shopId, batchSize, restoreAfterCreate);
    }
}