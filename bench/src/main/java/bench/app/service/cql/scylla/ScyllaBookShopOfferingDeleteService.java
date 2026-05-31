package bench.app.service.cql.scylla;

import bench.app.model.common.BookShopOfferingDeleteByUserResult;
import bench.app.service.cql.cassandra.CassandraBookShopOfferingDeleteService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScyllaBookShopOfferingDeleteService {
    private final CassandraBookShopOfferingDeleteService delegate;

    public ScyllaBookShopOfferingDeleteService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookShopOfferingDeleteService(scyllaSession);
    }

    public BookShopOfferingDeleteByUserResult deleteOfferingsForUserPermanentlyBorrowedBooks(UUID userId, boolean restoreAfterDelete) {
        return this.delegate.deleteOfferingsForUserPermanentlyBorrowedBooks(userId, restoreAfterDelete);
    }
}
