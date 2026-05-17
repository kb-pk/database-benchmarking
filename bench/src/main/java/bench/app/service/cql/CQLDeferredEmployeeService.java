package bench.app.service.cql;

import bench.app.model.cassandra.EmployeeByShop;
import bench.app.model.common.Employee;
import bench.app.model.common.LazilyInstantiated;
import bench.app.repository.cql.EmployeesByShopRepository;

import java.util.UUID;

public abstract class CQLDeferredEmployeeService<
        EmployeesByShopRepo extends EmployeesByShopRepository
        > {
    protected abstract EmployeesByShopRepo getEmployeeByShopRepo();

    protected LazilyInstantiated<Employee, bench.app.model.common.BookShop> getDeferredEmployee(UUID id) {
        EmployeeByShop e = this.getEmployeeByShopRepo().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        return bs -> new Employee(
                bs,
                e.getName(),
                e.getSurname(),
                e.getPhoneNumber(),
                e.getEmail(),
                e.getBirthDate(),
                e.getStartedAt(),
                e.getPrimaryBusinessRole()
        );
    }
}
