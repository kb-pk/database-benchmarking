package bench.app;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

@Component
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

        if (isBlank(loadSqlArg)) {
            return;
        }

        String fileName = loadSqlArg;

        Path sqlPath = resolveSqlPath(fileName);
        if (sqlPath == null) {
            throw new IllegalArgumentException("Nie znaleziono pliku SQL: " + fileName);
        }

        ensureSchemaCompatibility();
        clearBenchSchema();
        executeSqlFile(sqlPath);
        System.out.println("[bench] Zaladowano dane z pliku: " + sqlPath.toAbsolutePath());
    }

    private void ensureSchemaCompatibility() {
        // Kompatybilnosc z wygenerowanymi dumpami, ktore zawieraja kolumne salary.
        jdbcTemplate.execute("ALTER TABLE IF EXISTS bench.employee ADD COLUMN IF NOT EXISTS salary INT");
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