package bench.app.repository.cql;

import bench.app.model.cassandra.EmployeeByShop;
import org.springframework.data.cassandra.repository.AllowFiltering;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeesByShopRepository extends CassandraRepository<EmployeeByShop, UUID> {
    @AllowFiltering
    Optional<EmployeeByShop> findByEmployeeId(UUID employeeId);
}
