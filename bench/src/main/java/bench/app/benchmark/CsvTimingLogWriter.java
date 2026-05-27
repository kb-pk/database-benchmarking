package bench.app.benchmark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class CsvTimingLogWriter {
    private static final String HEADER = "timestamp,request_id,http_method,path,db_engine,http_status,total_ms,crud_ops_count,crud_ops,crud_durations_ms,error";
    private final Path outputRoot;

    public CsvTimingLogWriter(@Value("${app.benchmark.output-dir:output_data}") String outputDir) {
        this.outputRoot = Path.of(outputDir);
    }

    public synchronized void append(RequestTimingSnapshot snapshot) {
        String engineFolderName = normalizeEngine(snapshot.dbEngine());
        Path engineDir = outputRoot.resolve(engineFolderName);
        Path csvFile = engineDir.resolve("crud_timings_" + engineFolderName + ".csv");

        try {
            Files.createDirectories(engineDir);

            if (Files.notExists(csvFile)) {
                Files.writeString(csvFile, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            }

            Files.writeString(csvFile, toCsvRow(snapshot) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write benchmark CSV row", exception);
        }
    }

    private String toCsvRow(RequestTimingSnapshot snapshot) {
        String operations = snapshot.operations().stream()
                .map(operation -> operation.operationType() + ":" + operation.operationName())
                .collect(Collectors.joining("|"));

        String durations = snapshot.operations().stream()
                .map(operation -> formatMillis(operation.elapsedMillis()))
                .collect(Collectors.joining("|"));

        return String.join(",",
                escape(snapshot.timestamp().toString()),
                escape(snapshot.requestId()),
                escape(snapshot.httpMethod()),
                escape(snapshot.path()),
                escape(normalizeEngine(snapshot.dbEngine())),
                String.valueOf(snapshot.httpStatus()),
                formatMillis(snapshot.totalElapsedMillis()),
                String.valueOf(snapshot.operations().size()),
                escape(operations),
                escape(durations),
                escape(snapshot.error())
        );
    }

    private String formatMillis(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private String normalizeEngine(String dbEngine) {
        if (dbEngine == null || dbEngine.isBlank()) {
            return "unknown";
        }
        return dbEngine.trim().toLowerCase(Locale.ROOT);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("|")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }
}
