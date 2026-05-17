package bench.app.controller;

import bench.app.model.common.BookShop;
import bench.app.service.cql.CassandraBookShopService;
import bench.app.service.cql.ScyllaBookShopService;
import bench.app.service.sql.MssqlBookShopService;
import bench.app.service.sql.PostgresBookShopService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookshop")
public class BookShopController {
    private final PostgresBookShopService postgresService;
    private final MssqlBookShopService mssqlService;
    private final CassandraBookShopService cassandraService;
    private final ScyllaBookShopService scyllaService;

    public BookShopController(
            PostgresBookShopService postgresService,
            MssqlBookShopService mssqlService,
            CassandraBookShopService cassandraService,
            ScyllaBookShopService scyllaService) {
        this.postgresService = postgresService;
        this.mssqlService = mssqlService;
        this.cassandraService = cassandraService;
        this.scyllaService = scyllaService;
    }

    @GetMapping
    public List<BookShop> getBookShops(@RequestParam String db) {
        switch (db) {
            case "POSTGRESQL" -> {
                return this.postgresService.getBookShops();
            }
            case "MSSQL" -> {
                return this.mssqlService.getBookShops();
            }
            case "CASSANDRA" -> {
                return this.cassandraService.getBookShops();
            }
            case "SCYLLA" -> {
                return this.scyllaService.getBookShops();
            }
            default -> throw new IllegalArgumentException("Unknown database: " + db);
        }
    }
}
