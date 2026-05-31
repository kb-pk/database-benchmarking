package bench.app.service.cql.scylla;

import bench.app.model.common.BookShopCreateResult;
import bench.app.service.cql.cassandra.CassandraBookShopCreateService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ScyllaBookShopCreateService {
    private final CassandraBookShopCreateService delegate;

    public ScyllaBookShopCreateService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookShopCreateService(scyllaSession);
    }

    public BookShopCreateResult createBookShop(String shopName, String address, String email, UUID managerId, boolean restoreAfterCreate) {
        return this.delegate.createBookShop(shopName, address, email, managerId, restoreAfterCreate);
    }
}