package bench.app.controller;

import bench.app.model.common.Employee;
import bench.app.service.cql.cassandra.CassandraEmployeeService;
import bench.app.service.cql.scylla.ScyllaEmployeeService;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee")
public class EmployeeController {
    private final CassandraEmployeeService cassandraEmployeeService;
    private final ScyllaEmployeeService scyllaEmployeeService;

    public EmployeeController(CassandraEmployeeService cassandraEmployeeService, ScyllaEmployeeService scyllaEmployeeService) {
        this.cassandraEmployeeService = cassandraEmployeeService;
        this.scyllaEmployeeService = scyllaEmployeeService;
    }

    @GetMapping("/{id}")
    public Employee getEmployee(@RequestParam String db, @PathVariable("id") UUID uuid) {
        return switch (db) {
            case "CASSANDRA" -> this.cassandraEmployeeService.getEmployee(uuid);
            case "SCYLLA" -> this.scyllaEmployeeService.getEmployee(uuid);
            default -> throw new IllegalArgumentException("Invalid db");
        };
    }
}
