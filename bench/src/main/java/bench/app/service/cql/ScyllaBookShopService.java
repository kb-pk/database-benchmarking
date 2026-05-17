package bench.app.service.cql;

import bench.app.repository.cql.scylla.ScyllaBookShopRepository;
import org.springframework.stereotype.Service;

@Service
public class ScyllaBookShopService extends CQLBookShopService<ScyllaBookShopRepository> {
    private final ScyllaBookShopRepository repo;

    public ScyllaBookShopService(ScyllaBookShopRepository repo) {
        this.repo = repo;
    }

    @Override
    public ScyllaBookShopRepository getRepository() {
        return this.repo;
    }
}
