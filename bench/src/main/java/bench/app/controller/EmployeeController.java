package bench.app.controller;

import bench.app.model.common.Employee;
import bench.app.model.common.EmployeeShopAssignmentUpdateResult;
import bench.app.service.cql.cassandra.CassandraAnalyticsService;
import bench.app.service.cql.cassandra.CassandraEmployeeService;
import bench.app.service.cql.scylla.ScyllaAnalyticsService;
import bench.app.service.cql.scylla.ScyllaEmployeeService;
import bench.app.service.sql.MssqlEmployeeUpdateService;
import bench.app.service.sql.PostgresEmployeeUpdateService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private final CassandraEmployeeService cassandraEmployeeService;
    private final ScyllaEmployeeService scyllaEmployeeService;
    private final CassandraAnalyticsService cassandraAnalyticsService;
    private final ScyllaAnalyticsService scyllaAnalyticsService;
    private final PostgresEmployeeUpdateService postgresEmployeeUpdateService;
    private final MssqlEmployeeUpdateService mssqlEmployeeUpdateService;

    public EmployeeController(
            CassandraEmployeeService cassandraEmployeeService,
            ScyllaEmployeeService scyllaEmployeeService,
            CassandraAnalyticsService cassandraAnalyticsService,
            ScyllaAnalyticsService scyllaAnalyticsService,
            PostgresEmployeeUpdateService postgresEmployeeUpdateService,
            MssqlEmployeeUpdateService mssqlEmployeeUpdateService
    ) {
        this.cassandraEmployeeService = cassandraEmployeeService;
        this.scyllaEmployeeService = scyllaEmployeeService;
        this.cassandraAnalyticsService = cassandraAnalyticsService;
        this.scyllaAnalyticsService = scyllaAnalyticsService;
        this.postgresEmployeeUpdateService = postgresEmployeeUpdateService;
        this.mssqlEmployeeUpdateService = mssqlEmployeeUpdateService;
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@RequestParam String db, @PathVariable("id") String id) {
        return switch (db) {
            case "CASSANDRA" -> this.cassandraEmployeeService.getEmployee(UUID.fromString(id));
            case "SCYLLA" -> this.scyllaEmployeeService.getEmployee(UUID.fromString(id));
            default -> throw new IllegalArgumentException("Invalid db");
        };
    }

    // U4: Przypisanie pracownika do innego sklepu (z przywróceniem poprzedniego stanu)
    @PostMapping("/{id}/primary-shop")
    public EmployeeShopAssignmentUpdateResult updateEmployeePrimaryShop(
            @RequestParam String db,
            @PathVariable("id") String id,
            @RequestParam String shopId,
            @RequestParam(defaultValue = "true") boolean restoreAfterUpdate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresEmployeeUpdateService
                .reassignEmployeeToShop(Long.parseLong(id), Long.parseLong(shopId), restoreAfterUpdate);
            case "MSSQL" -> this.mssqlEmployeeUpdateService
                .reassignEmployeeToShop(Long.parseLong(id), Long.parseLong(shopId), restoreAfterUpdate);
                case "CASSANDRA" -> this.cassandraAnalyticsService
                .reassignEmployeeToShop(UUID.fromString(id), UUID.fromString(shopId), restoreAfterUpdate);
                case "SCYLLA" -> this.scyllaAnalyticsService
                .reassignEmployeeToShop(UUID.fromString(id), UUID.fromString(shopId), restoreAfterUpdate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }
}
