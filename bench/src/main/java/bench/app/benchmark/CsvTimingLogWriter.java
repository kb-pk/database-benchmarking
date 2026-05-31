package bench.app.benchmark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CsvTimingLogWriter {
    private static final String HEADER = "operation,iteration_1_ms,iteration_2_ms,iteration_3_ms";
    private final Path outputRoot;

    // Czyści plik CSV dla wybranego silnika (zostawia tylko nagłówek)
    public synchronized void clearCsvForEngine(String dbEngine) {
        String engineFolderName = normalizeEngine(dbEngine);
        Path engineDir = outputRoot.resolve(engineFolderName);
        Path csvFile = engineDir.resolve("crud_timings_" + engineFolderName + ".csv");
        try {
            Files.createDirectories(engineDir);
            Files.writeString(csvFile, HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie można wyczyścić pliku CSV", exception);
        }
    }

    public CsvTimingLogWriter(@Value("${app.benchmark.output-dir:output_data}") String outputDir) {
        this.outputRoot = Path.of(outputDir);
    }

    public synchronized void append(RequestTimingSnapshot snapshot) {
        String engineFolderName = normalizeEngine(snapshot.dbEngine());
        Path engineDir = outputRoot.resolve(engineFolderName);
        Path csvFile = engineDir.resolve("crud_timings_" + engineFolderName + ".csv");

        try {
            Files.createDirectories(engineDir);

            Map<String, String[]> rows = readExistingRows(csvFile);
            upsertOperationRow(rows, snapshot);
            writeRows(csvFile, rows);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write benchmark CSV row", exception);
        }
    }

    private Map<String, String[]> readExistingRows(Path csvFile) throws IOException {
        Map<String, String[]> rows = new LinkedHashMap<>();

        if (Files.notExists(csvFile)) {
            return rows;
        }

        List<String> lines = Files.readAllLines(csvFile, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return rows;
        }

        if (!HEADER.equals(lines.get(0).trim())) {
            return rows;
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split(",", -1);
            if (parts.length < 4) {
                continue;
            }

            String operation = unescape(parts[0]);
            rows.put(operation, new String[]{parts[1], parts[2], parts[3]});
        }

        return rows;
    }

    private void upsertOperationRow(Map<String, String[]> rows, RequestTimingSnapshot snapshot) {
        String operation = normalizeOperationLabel(snapshot);
        String[] iterations = rows.computeIfAbsent(operation, key -> new String[]{"", "", ""});
        String elapsed = formatMillis(snapshot.totalElapsedMillis());

        Integer iteration = snapshot.iteration();
        if (iteration != null) {
            if (iteration == 1) {
                return;
            }

            int targetIndex = iteration - 2;
            if (targetIndex >= 0 && targetIndex < iterations.length) {
                iterations[targetIndex] = elapsed;
            }
            return;
        }

        for (int i = 0; i < iterations.length; i++) {
            if (iterations[i] == null || iterations[i].isBlank()) {
                iterations[i] = elapsed;
                return;
            }
        }

        iterations[2] = elapsed;
    }

    private void writeRows(Path csvFile, Map<String, String[]> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);

        for (Map.Entry<String, String[]> entry : rows.entrySet()) {
            String[] iterations = entry.getValue();
            lines.add(String.join(",",
                    escape(entry.getKey()),
                    safeCell(iterations[0]),
                    safeCell(iterations[1]),
                    safeCell(iterations[2])
            ));
        }

        Files.write(csvFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private String safeCell(String value) {
        if (value == null) {
            return "";
        }
        return value;
    }

    private String normalizeOperationLabel(RequestTimingSnapshot snapshot) {
        String operation = snapshot.operationLabel();
        if (operation == null || operation.isBlank()) {
            return snapshot.httpMethod() + " " + snapshot.path();
        }
        return operation.trim();
    }

    private String formatMillis(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private String normalizeEngine(String dbEngine) {
        if (dbEngine == null || dbEngine.isBlank()) {
            return "unknown";
        }

        String normalized = dbEngine
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z]", "");

        return switch (normalized) {
            case "postgresql", "postgres", "psql" -> "postgresql";
            case "mssql", "sqlserver", "sqlsrv" -> "mssql";
            case "cassandra" -> "cassandra";
            case "scylla", "scylladb" -> "scylla";
            default -> "unknown";
        };
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

    private String unescape(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }
}
