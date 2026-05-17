package bench.app.service.cql;

import bench.app.model.cassandra.BookShop;
import bench.app.model.cassandra.EmployeeByShop;
import bench.app.model.common.Employee;
import bench.app.repository.cql.BooksByShopRepository;
import bench.app.repository.cql.EmployeesByShopRepository;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.UUID;

public abstract class CQLEmployeeService<
        EmployeesByShopRepo extends EmployeesByShopRepository,
        BookShopRepo extends CassandraRepository<BookShop, UUID>,
        BooksByShopRepo extends BooksByShopRepository,
        DeferredEmployeeService extends CQLDeferredEmployeeService<EmployeesByShopRepo>,
        BookShopService extends CQLBookShopService<BookShopRepo, BooksByShopRepo, EmployeesByShopRepo, DeferredEmployeeService>
        > {
    protected abstract EmployeesByShopRepo getEmployeeByShopRepo();
    protected abstract BookShopService getBookShopService();

    public Employee getEmployee(UUID id) {
        EmployeeByShop e = this.getEmployeeByShopRepo().findByEmployeeId(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        bench.app.model.common.BookShop shop = this.getBookShopService().getBookShop(e.getPrimaryBookShopId());
        return this.getBookShopService().getDeferredEmployeeService().getDeferredEmployee(id).instantiateWith(shop);
    }
}
