package bench.app.service.cql.scylla;

import bench.app.repository.cql.scylla.ScyllaEmployeesByShopRepository;
import bench.app.service.cql.CQLDeferredEmployeeService;
import org.springframework.stereotype.Service;

@Service
public class ScyllaDeferredEmployeeService extends CQLDeferredEmployeeService<ScyllaEmployeesByShopRepository> {
    private final ScyllaEmployeesByShopRepository scyllaEmployeesByShopRepository;

    public ScyllaDeferredEmployeeService(ScyllaEmployeesByShopRepository scyllaEmployeesByShopRepository) {
        this.scyllaEmployeesByShopRepository = scyllaEmployeesByShopRepository;
    }

    @Override
    protected ScyllaEmployeesByShopRepository getEmployeeByShopRepo() {
        return this.scyllaEmployeesByShopRepository;
    }
}
