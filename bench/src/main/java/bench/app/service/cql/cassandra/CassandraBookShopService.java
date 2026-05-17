package bench.app.service.cql.cassandra;

import bench.app.repository.cql.cassandra.CassandraBookShopRepository;
import bench.app.repository.cql.cassandra.CassandraBooksByShopRepository;
import bench.app.repository.cql.cassandra.CassandraEmployeesByShopRepository;
import bench.app.service.cql.CQLBookShopService;
import org.springframework.stereotype.Service;

@Service
public class CassandraBookShopService extends CQLBookShopService<
        CassandraBookShopRepository,
        CassandraBooksByShopRepository,
        CassandraEmployeesByShopRepository,
        CassandraDeferredEmployeeService
        > {
    private final CassandraBookShopRepository cassandraBookShopRepository;
    private final CassandraBooksByShopRepository cassandraBooksByShopRepository;
    private final CassandraDeferredEmployeeService cassandraDeferredEmployeeService;

    public CassandraBookShopService(
            CassandraBookShopRepository cassandraBookShopRepository,
            CassandraBooksByShopRepository cassandraBooksByShopRepository,
            CassandraDeferredEmployeeService cassandraDeferredEmployeeService) {
        this.cassandraBookShopRepository = cassandraBookShopRepository;
        this.cassandraBooksByShopRepository = cassandraBooksByShopRepository;
        this.cassandraDeferredEmployeeService = cassandraDeferredEmployeeService;
    }

    @Override
    protected CassandraBookShopRepository getBookShopRepo() {
        return this.cassandraBookShopRepository;
    }

    @Override
    protected CassandraBooksByShopRepository getBooksByShopRepo() {
        return this.cassandraBooksByShopRepository;
    }

    @Override
    protected CassandraDeferredEmployeeService getDeferredEmployeeService() {
        return this.cassandraDeferredEmployeeService;
    }
}
