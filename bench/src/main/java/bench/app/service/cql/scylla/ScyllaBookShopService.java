package bench.app.service.cql.scylla;

import bench.app.repository.cql.scylla.ScyllaBookShopRepository;
import bench.app.repository.cql.scylla.ScyllaBooksByShopRepository;
import bench.app.repository.cql.scylla.ScyllaEmployeesByShopRepository;
import bench.app.service.cql.CQLBookShopService;
import org.springframework.stereotype.Service;

@Service
public class ScyllaBookShopService extends CQLBookShopService<
        ScyllaBookShopRepository,
        ScyllaBooksByShopRepository,
        ScyllaEmployeesByShopRepository,
        ScyllaDeferredEmployeeService
        > {
    private final ScyllaBookShopRepository scyllaBookShopRepository;
    private final ScyllaBooksByShopRepository scyllaBooksByShopRepository;
    private final ScyllaDeferredEmployeeService scyllaDeferredEmployeeService;

    public ScyllaBookShopService(
            ScyllaBookShopRepository scyllaBookShopRepository,
            ScyllaBooksByShopRepository scyllaBooksByShopRepository,
            ScyllaDeferredEmployeeService scyllaDeferredEmployeeService) {
        this.scyllaBookShopRepository = scyllaBookShopRepository;
        this.scyllaBooksByShopRepository = scyllaBooksByShopRepository;
        this.scyllaDeferredEmployeeService = scyllaDeferredEmployeeService;
    }

    @Override
    protected ScyllaBookShopRepository getBookShopRepo() {
        return this.scyllaBookShopRepository;
    }

    @Override
    protected ScyllaBooksByShopRepository getBooksByShopRepo() {
        return this.scyllaBooksByShopRepository;
    }

    @Override
    protected ScyllaDeferredEmployeeService getDeferredEmployeeService() {
        return this.scyllaDeferredEmployeeService;
    }
}
