package bench.app.service.cql;

import bench.app.model.cassandra.EmployeeByShop;
import bench.app.model.common.Employee;
import bench.app.model.common.LazilyInstantiated;
import bench.app.repository.cql.EmployeesByShopRepository;

import java.sql.Date;
import java.util.UUID;

public abstract class CQLDeferredEmployeeService<
        EmployeesByShopRepo extends EmployeesByShopRepository
        > {
    protected abstract EmployeesByShopRepo getEmployeeByShopRepo();

    protected LazilyInstantiated<Employee, bench.app.model.common.BookShop> getDeferredEmployee(UUID id) {
        EmployeeByShop e = this.getEmployeeByShopRepo().findByEmployeeId(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        return bs -> new Employee(
                bs,
                e.getName(),
                e.getSurname(),
                e.getPhoneNumber(),
                e.getEmail(),
                e.getBirthDate() != null ? Date.valueOf(e.getBirthDate()) : null,
                e.getStartedAt() != null ? Date.valueOf(e.getStartedAt()) : null,
                e.getPrimaryBusinessRole()
        );
    }
}
