package bench.app.service.userpermission;

import bench.app.util.CsvCrudLogger;
import jakarta.annotation.PreDestroy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class JdbcUserPermissionCrudService implements UserPermissionCrudEngineService {
    private static final int CREATE_REPETITIONS = 3;

    private final JdbcTemplate jdbcTemplate;
    private final UserPermissionQueryCatalog queryCatalog;
    private final BenchmarkEngineResolver engineResolver;
    private final AtomicInteger createOperationCounter = new AtomicInteger(1);
    private final AtomicInteger readOperationCounter = new AtomicInteger(1);
    private final AtomicInteger updateOperationCounter = new AtomicInteger(1);
    private final AtomicInteger deleteOperationCounter = new AtomicInteger(1);

    private CsvCrudLogger csvLogger;

    public JdbcUserPermissionCrudService(
            JdbcTemplate jdbcTemplate,
            UserPermissionQueryCatalog queryCatalog,
            BenchmarkEngineResolver engineResolver
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.queryCatalog = queryCatalog;
        this.engineResolver = engineResolver;
    }

    @Override
    public boolean supports(DatabaseEngine engine) {
        return engine == DatabaseEngine.POSTGRESQL || engine == DatabaseEngine.MSSQL;
    }

    @Override
    public Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details) {
        validatePermission(permission);

        Integer maxId = jdbcTemplate.queryForObject(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.SELECT_MAX_ID),
                Integer.class
        );
        int nextId = (maxId == null ? 0 : maxId) + 1;
        int firstIdToInsert = (requestedId == null || requestedId <= 0) ? nextId : requestedId;
        List<Integer> insertedIds = new ArrayList<>();
        List<Instant> starts = new ArrayList<>();
        List<Instant> ends = new ArrayList<>();
        double totalDurationMs = 0.0;

        try {
            for (int i = 0; i < CREATE_REPETITIONS; i++) {
                int currentId = firstIdToInsert + i;
                Instant start = Instant.now();
                int affectedRows = jdbcTemplate.update(
                        queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.CREATE),
                        currentId,
                        permission,
                        details
                );
                Instant end = Instant.now();

                if (affectedRows != 1) {
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "insert failed");
                }

                double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
                totalDurationMs += durationMs;
                insertedIds.add(currentId);
                starts.add(start);
                ends.add(end);
            }

            double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
            logCreate(engine, starts, ends, averageDurationMs);

            return Map.of(
                    "status", "created",
                    "engine", engine.propertyValue(),
                    "executions", CREATE_REPETITIONS,
                    "ids", insertedIds,
                    "average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs),
                    "permission", permission
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create permission: " + ex.getMessage());
        }
    }

    @Override
    public Map<String, Object> read(DatabaseEngine engine, int id) {
        try {
            Instant start = Instant.now();
            Map<String, Object> row = jdbcTemplate.queryForMap(
                    queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.READ),
                    id
            );
            Instant end = Instant.now();

            logSingle(engine, CsvCrudLogger.CrudType.READ, readOperationCounter.getAndIncrement(), start, end);

            return Map.of(
                    "status", "read",
                    "engine", engine.propertyValue(),
                    "data", row
            );
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }
    }

    @Override
    public Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details) {
        validatePermission(permission);

        Instant start = Instant.now();
        int affectedRows = jdbcTemplate.update(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.UPDATE),
                permission,
                details,
                id
        );
        Instant end = Instant.now();

        if (affectedRows != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }

        logSingle(engine, CsvCrudLogger.CrudType.UPDATE, updateOperationCounter.getAndIncrement(), start, end);

        return Map.of(
                "status", "updated",
                "engine", engine.propertyValue(),
                "id", id,
                "permission", permission,
                "details", details
        );
    }

    @Override
    public Map<String, Object> delete(DatabaseEngine engine, int id) {
        Instant start = Instant.now();
        int affectedRows = jdbcTemplate.update(
                queryCatalog.getRequiredQuery(engine, UserPermissionQueryType.DELETE),
                id
        );
        Instant end = Instant.now();

        if (affectedRows != 1) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "permission not found for id=" + id);
        }

        logSingle(engine, CsvCrudLogger.CrudType.DELETE, deleteOperationCounter.getAndIncrement(), start, end);

        return Map.of(
                "status", "deleted",
                "engine", engine.propertyValue(),
                "id", id
        );
    }

    @PreDestroy
    public void closeLogger() {
        if (csvLogger == null) {
            return;
        }

        try {
            csvLogger.close();
        } catch (IOException e) {
            System.err.println("Nie mozna zamknac loggera CSV: " + e.getMessage());
        }
    }

    private void validatePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permission is required");
        }
    }

    private void logCreate(DatabaseEngine engine, List<Instant> starts, List<Instant> ends, double averageDurationMs) {
        try {
            getLogger(engine).logCreate(
                    createOperationCounter.getAndIncrement(),
                    starts.get(0), ends.get(0),
                    starts.get(1), ends.get(1),
                    starts.get(2), ends.get(2),
                    averageDurationMs
            );
        } catch (IOException e) {
            System.err.println("Blad logowania CREATE do CSV: " + e.getMessage());
        }
    }

    private void logSingle(DatabaseEngine engine, CsvCrudLogger.CrudType crudType, int operationNumber, Instant start, Instant end) {
        try {
            getLogger(engine).log(crudType, operationNumber, start, end);
        } catch (IOException e) {
            System.err.println("Blad logowania do CSV: " + e.getMessage());
        }
    }

    private synchronized CsvCrudLogger getLogger(DatabaseEngine engine) throws IOException {
        if (csvLogger == null) {
            csvLogger = new CsvCrudLogger(engine.propertyValue(), engineResolver.resolveDatasetSize());
        }
        return csvLogger;
    }
}
