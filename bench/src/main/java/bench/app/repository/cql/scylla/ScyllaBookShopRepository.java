package bench.app.repository.cql.scylla;

import bench.app.model.cassandra.BookShop;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ScyllaBookShopRepository extends CassandraRepository<BookShop, UUID>  {
}
