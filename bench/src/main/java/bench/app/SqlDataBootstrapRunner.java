package bench.app;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

@Component
@Order(1)
public class SqlDataBootstrapRunner implements ApplicationRunner {
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SqlDataBootstrapRunner(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String loadSqlArg = getFirstOption(args, "bench.load-sql");
        String engineArg = getFirstOption(args, "bench.engine");

        if (isBlank(loadSqlArg)) {
            return;
        }

        String fileName = loadSqlArg;
        String engine = isBlank(engineArg) ? extractEngineFromFileName(fileName) : engineArg;

        Path sqlPath = resolveSqlPath(fileName);
        if (sqlPath == null) {
            throw new IllegalArgumentException("Nie znaleziono pliku SQL: " + fileName);
        }

        clearOldCsvFiles(engine);
        
        if ("postgresql".equalsIgnoreCase(engine)) {
            ensureSchemaCompatibility();
            clearBenchSchema();
        } else if ("mssql".equalsIgnoreCase(engine)) {
            ensureSchemaForMssql();
        }
        
        executeSqlFile(sqlPath);
        System.out.println("[bench] Zaladowano dane z pliku: " + sqlPath.toAbsolutePath());
    }

    private void ensureSchemaCompatibility() {
        // Kompatybilnosc z wygenerowanymi dumpami, ktore zawieraja kolumne salary.
        jdbcTemplate.execute("ALTER TABLE IF EXISTS bench.employee ADD COLUMN IF NOT EXISTS salary INT");
    }

    private void clearOldCsvFiles(String engine) {
        try {
            String folderName = mapEngineToPreciseFolder(engine);
            String[] sizes = {"1000", "10000", "100000", "250000", "500000", "1000000", "2000000", "5000000", "10000000"};
            String[] crudTypes = {"create", "read", "update", "delete"};

            Path outputDir = Path.of("..", "output_data", folderName);
            
            for (String size : sizes) {
                for (String type : crudTypes) {
                    String fileName = engine + "_" + size + "_" + type + ".csv";
                    Path filePath = outputDir.resolve(fileName);
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                        System.out.println("[bench] Usunieto stary plik CSV: " + fileName);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[bench] Blad podczas czyszczenia starych plikow CSV: " + e.getMessage());
        }
    }

    private String extractEngineFromFileName(String fileName) {
        // Parsuj engine z nazwy pliku: inserts_{engine}_{size}.sql
        // np. "inserts_postgresql_1000.sql" -> "postgresql"
        if (fileName == null || fileName.isEmpty()) {
            return "postgresql"; // domyślnie
        }
        
        String baseName = fileName.replaceAll("\\.(sql|cql)$", "");
        String[] parts = baseName.split("_");
        
        if (parts.length >= 2) {
            return parts[1]; // drugi element to engine
        }
        
        return "postgresql"; // domyślnie
    }

    private String mapEngineToPreciseFolder(String engine) {
        // Mapuj engine names na folder names w output_data
        return switch (engine.toLowerCase()) {
            case "postgresql" -> "postgre";
            case "mssql" -> "mssql";
            case "cassandra" -> "cassandra";
            case "scylla" -> "scylla";
            default -> engine;
        };
    }

    private void clearBenchSchema() {
        String tables = jdbcTemplate.queryForObject(
                "SELECT string_agg(format('%I.%I', schemaname, tablename), ', ') " +
                        "FROM pg_tables WHERE schemaname = 'bench'",
                String.class
        );

        if (isBlank(tables)) {
            System.out.println("[bench] Brak tabel do czyszczenia w schemacie bench.");
            return;
        }

        jdbcTemplate.execute("TRUNCATE TABLE " + tables + " RESTART IDENTITY CASCADE");
        System.out.println("[bench] Wyczyszczono dane ze wszystkich tabel schematu bench.");
    }

    private void executeSqlFile(Path sqlPath) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection, new FileSystemResource(sqlPath));
        }
    }

    private Path resolveSqlPath(String fileNameOrPath) {
        Path directPath = Path.of(fileNameOrPath);
        if (Files.exists(directPath)) {
            return directPath;
        }

        Path generatedFromBenchDir = Path.of("generated_sql", fileNameOrPath);
        if (Files.exists(generatedFromBenchDir)) {
            return generatedFromBenchDir;
        }

        Path generatedFromProjectDir = Path.of("..", "generated_sql", fileNameOrPath);
        if (Files.exists(generatedFromProjectDir)) {
            return generatedFromProjectDir;
        }

        return null;
    }

    private void ensureSchemaForMssql() throws Exception {
        ensureMssqlSchemaExists();
        resetMssqlBenchSchemaObjects();

        // Zaladuj strukture tabel dla MSSQL
        Path schemaPath = Path.of("..", "schema", "sql", "create_schema_structure_mssql.sql");
        if (!Files.exists(schemaPath)) {
            schemaPath = Path.of("schema", "sql", "create_schema_structure_mssql.sql");
        }

        if (Files.exists(schemaPath)) {
            executeSqlFile(schemaPath);
            System.out.println("[bench] Zaladowano schemat MSSQL: " + schemaPath.toAbsolutePath());
        } else {
            System.err.println("[bench] UWAGA: Nie znaleziono pliku schematu MSSQL");
        }
    }

    private void ensureMssqlSchemaExists() {
        try {
            Integer schemaCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys.schemas WHERE name = 'bench'",
                    Integer.class
            );

            if (schemaCount == null || schemaCount == 0) {
                jdbcTemplate.execute("CREATE SCHEMA bench");
                System.out.println("[bench] Utworzono schemat MSSQL: bench");
            }
        } catch (Exception e) {
            throw new IllegalStateException("[bench] Blad podczas tworzenia schematu MSSQL", e);
        }
    }

    private void resetMssqlBenchSchemaObjects() {
        try {
            var foreignKeyRows = jdbcTemplate.queryForList(
                    "SELECT fk.name AS fk_name, t.name AS table_name " +
                            "FROM sys.foreign_keys fk " +
                            "JOIN sys.tables t ON fk.parent_object_id = t.object_id " +
                            "JOIN sys.schemas s ON t.schema_id = s.schema_id " +
                            "WHERE s.name = 'bench'"
            );

            for (var row : foreignKeyRows) {
                String fkName = String.valueOf(row.get("fk_name"));
                String tableName = String.valueOf(row.get("table_name"));
                jdbcTemplate.execute("ALTER TABLE bench.[" + tableName + "] DROP CONSTRAINT [" + fkName + "]");
            }

            var tableRows = jdbcTemplate.queryForList(
                    "SELECT t.name AS table_name " +
                            "FROM sys.tables t " +
                            "JOIN sys.schemas s ON t.schema_id = s.schema_id " +
                            "WHERE s.name = 'bench'"
            );

            for (var row : tableRows) {
                String tableName = String.valueOf(row.get("table_name"));
                jdbcTemplate.execute("DROP TABLE bench.[" + tableName + "]");
            }

            if (!tableRows.isEmpty()) {
                System.out.println("[bench] Zresetowano obiekty schematu MSSQL bench.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("[bench] Blad podczas resetu obiektow MSSQL", e);
        }
    }

    private String getFirstOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            return null;
        }
        var values = args.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}