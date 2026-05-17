package bench.app.service.cql;

import bench.app.repository.cql.cassandra.CassandraBookShopRepository;
import org.springframework.stereotype.Service;

@Service
public class CassandraBookShopService extends CQLBookShopService<CassandraBookShopRepository> {
    private final CassandraBookShopRepository cassandraBookShopRepository;

    public CassandraBookShopService(
            CassandraBookShopRepository cassandraBookShopRepository) {
        this.cassandraBookShopRepository = cassandraBookShopRepository;
    }

    @Override
    public CassandraBookShopRepository getRepository() {
        return this.cassandraBookShopRepository;
    }
}
