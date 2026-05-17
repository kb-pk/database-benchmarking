package bench.app.service.cql.cassandra;

import bench.app.repository.cql.cassandra.CassandraBookShopRepository;
import bench.app.repository.cql.cassandra.CassandraBooksByShopRepository;
import bench.app.repository.cql.cassandra.CassandraEmployeesByShopRepository;
import bench.app.service.cql.CQLEmployeeService;
import org.springframework.stereotype.Service;

@Service
public class CassandraEmployeeService extends CQLEmployeeService<
        CassandraEmployeesByShopRepository,
        CassandraBookShopRepository,
        CassandraBooksByShopRepository,
        CassandraDeferredEmployeeService,
        CassandraBookShopService
        > {
    private final CassandraEmployeesByShopRepository cassandraEmployeesByShopRepository;
    private final CassandraBookShopService cassandraBookShopService;

    public CassandraEmployeeService(
            CassandraEmployeesByShopRepository cassandraEmployeesByShopRepository,
            CassandraBookShopService cassandraBookShopService) {
        this.cassandraEmployeesByShopRepository = cassandraEmployeesByShopRepository;
        this.cassandraBookShopService = cassandraBookShopService;
    }

    @Override
    protected CassandraEmployeesByShopRepository getEmployeeByShopRepo() {
        return this.cassandraEmployeesByShopRepository;
    }

    @Override
    protected CassandraBookShopService getBookShopService() {
        return this.cassandraBookShopService;
    }
}
