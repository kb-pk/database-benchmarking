package bench.app.service.userpermission;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class BenchmarkEngineResolver {
    private static final Pattern LOAD_SQL_PATTERN = Pattern.compile("inserts_([a-zA-Z0-9]+)_([0-9]+)\\.(sql|cql)");

    private final String configuredEngine;
    private final String loadSqlFile;

    public BenchmarkEngineResolver(
            @Value("${bench.engine:}") String configuredEngine,
            @Value("${bench.load-sql:inserts_postgresql_10000.sql}") String loadSqlFile
    ) {
        this.configuredEngine = configuredEngine;
        this.loadSqlFile = loadSqlFile;
    }

    public DatabaseEngine resolveEngine() {
        if (configuredEngine != null && !configuredEngine.isBlank()) {
            return DatabaseEngine.fromValue(configuredEngine);
        }

        Matcher matcher = LOAD_SQL_PATTERN.matcher(Paths.get(loadSqlFile).getFileName().toString());
        if (matcher.matches()) {
            return DatabaseEngine.fromValue(matcher.group(1));
        }

        return DatabaseEngine.POSTGRESQL;
    }

    public String resolveDatasetSize() {
        Matcher matcher = LOAD_SQL_PATTERN.matcher(Paths.get(loadSqlFile).getFileName().toString());
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return "10000";
    }
}
