package bench.app.service.cql.cassandra;

import bench.app.repository.cql.cassandra.CassandraEmployeesByShopRepository;
import bench.app.service.cql.CQLDeferredEmployeeService;
import org.springframework.stereotype.Service;

@Service
public class CassandraDeferredEmployeeService extends CQLDeferredEmployeeService<CassandraEmployeesByShopRepository> {
    private final CassandraEmployeesByShopRepository repo;

    public CassandraDeferredEmployeeService(CassandraEmployeesByShopRepository repo) {
        this.repo = repo;
    }

    @Override
    protected CassandraEmployeesByShopRepository getEmployeeByShopRepo() {
        return this.repo;
    }
}
