package bench.app.repository.cql;

import bench.app.model.cassandra.BookByShop;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public interface BooksByShopRepository extends CassandraRepository<BookByShop, UUID> {
    List<BookByShop> findByShopId(UUID shopId);
}
