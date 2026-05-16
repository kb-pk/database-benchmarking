package bench.app.repository.cql.cassandra;

import bench.app.model.cassandra.BookShop;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CassandraBookShopRepository extends CassandraRepository<BookShop, UUID> {
}
