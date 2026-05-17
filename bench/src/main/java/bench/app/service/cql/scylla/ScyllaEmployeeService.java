package bench.app.service.cql.scylla;

import bench.app.repository.cql.scylla.ScyllaBookShopRepository;
import bench.app.repository.cql.scylla.ScyllaBooksByShopRepository;
import bench.app.repository.cql.scylla.ScyllaEmployeesByShopRepository;
import bench.app.service.cql.CQLEmployeeService;
import org.springframework.stereotype.Service;

@Service
public class ScyllaEmployeeService extends CQLEmployeeService<
        ScyllaEmployeesByShopRepository,
        ScyllaBookShopRepository,
        ScyllaBooksByShopRepository,
        ScyllaDeferredEmployeeService,
        ScyllaBookShopService
        > {
    private final ScyllaEmployeesByShopRepository scyllaEmployeesByShopRepository;
    private final ScyllaBookShopService scyllaBookShopService;

    public ScyllaEmployeeService(
            ScyllaEmployeesByShopRepository scyllaEmployeesByShopRepository,
            ScyllaBookShopService scyllaBookShopService) {
        this.scyllaEmployeesByShopRepository = scyllaEmployeesByShopRepository;
        this.scyllaBookShopService = scyllaBookShopService;
    }

    @Override
    protected ScyllaEmployeesByShopRepository getEmployeeByShopRepo() {
        return this.scyllaEmployeesByShopRepository;
    }

    @Override
    protected ScyllaBookShopService getBookShopService() {
        return this.scyllaBookShopService;
    }
}
