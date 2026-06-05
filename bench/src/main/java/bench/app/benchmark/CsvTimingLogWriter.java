package bench.app.benchmark;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class CsvTimingLogWriter {
    private static final String SUMMARY_HEADER = "operation,iteration_1_ms,iteration_2_ms,iteration_3_ms";
    private static final String RAW_HEADER = "timestamp,phase,operation,iteration,total_ms,repository_ms,overhead_ms,dominant_repository_operation,dominant_repository_ms,http_status,error";
    private final Path outputRoot;

    // Czyści plik CSV dla wybranego silnika (zostawia tylko nagłówek)
    public synchronized void clearCsvForEngine(String dbEngine) {
        String engineFolderName = normalizeEngine(dbEngine);
        Path engineDir = outputRoot.resolve(engineFolderName);
        Path summaryFile = engineDir.resolve("crud_timings_" + engineFolderName + ".csv");
        Path rawFile = engineDir.resolve("crud_timings_" + engineFolderName + "_raw.csv");
        try {
            Files.createDirectories(engineDir);
            Files.writeString(summaryFile, SUMMARY_HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            Files.writeString(rawFile, RAW_HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
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
        Path summaryFile = engineDir.resolve("crud_timings_" + engineFolderName + ".csv");
        Path rawFile = engineDir.resolve("crud_timings_" + engineFolderName + "_raw.csv");

        try {
            Files.createDirectories(engineDir);
            appendRawRow(rawFile, snapshot);
            writeSummary(summaryFile, loadMeasuredSamples(rawFile));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write benchmark CSV row", exception);
        }
    }

    private void appendRawRow(Path rawFile, RequestTimingSnapshot snapshot) throws IOException {
        if (Files.notExists(rawFile)) {
            Files.writeString(rawFile, RAW_HEADER + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        }

        RawTimingRow rawTimingRow = RawTimingRow.from(snapshot, normalizeOperationLabel(snapshot), isWarmup(snapshot));
        Files.writeString(
                rawFile,
                rawTimingRow.toCsvLine() + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND,
                StandardOpenOption.WRITE
        );
    }

    private Map<String, String[]> loadMeasuredSamples(Path rawFile) throws IOException {
        Map<String, String[]> rows = new LinkedHashMap<>();
        if (Files.notExists(rawFile)) {
            return rows;
        }

        List<String> lines = Files.readAllLines(rawFile, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !RAW_HEADER.equals(lines.get(0).trim())) {
            return rows;
        }

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }

            String[] parts = splitCsv(line, 11);
            if (parts.length < 11) {
                continue;
            }
            String phase = unescape(parts[1]);
            if (!"measured".equals(phase)) {
                continue;
            }

            String operation = unescape(parts[2]);
            String totalMillis = unescape(parts[4]);
            String[] iterations = rows.computeIfAbsent(operation, key -> new String[]{"", "", ""});
            for (int index = 0; index < iterations.length; index++) {
                if (iterations[index] == null || iterations[index].isBlank()) {
                    iterations[index] = totalMillis;
                    break;
                }
            }
        }

        return rows;
    }

    private void writeSummary(Path summaryFile, Map<String, String[]> rows) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(SUMMARY_HEADER);

        for (Map.Entry<String, String[]> entry : rows.entrySet()) {
            String[] iterations = entry.getValue();
            lines.add(String.join(",",
                    escape(entry.getKey()),
                    safeCell(iterations[0]),
                    safeCell(iterations[1]),
                    safeCell(iterations[2])
            ));
        }

        Files.write(summaryFile, lines, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
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

    private String safeCell(String value) {
        return value == null ? "" : value;
    }

    private boolean isWarmup(RequestTimingSnapshot snapshot) {
        Integer iteration = snapshot.iteration();
        if (iteration == null) {
            return false;
        }

        Integer warmupIterations = snapshot.warmupIterations();
        int limit = warmupIterations == null || warmupIterations < 0 ? 1 : warmupIterations;
        return iteration <= limit;
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

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\r") || escaped.contains("|")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private static String unescape(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
        }
        return trimmed;
    }

    private static String[] splitCsv(String line, int expectedColumns) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if (currentChar == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (currentChar == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(currentChar);
            }
        }

        values.add(current.toString());
        while (values.size() < expectedColumns) {
            values.add("");
        }
        return values.toArray(String[]::new);
    }

    private record RawTimingRow(
            String timestamp,
            String phase,
            String operation,
            String iteration,
            String totalMillis,
            String repositoryMillis,
            String overheadMillis,
            String dominantRepositoryOperation,
            String dominantRepositoryMillis,
            String httpStatus,
            String error
    ) {
        private static RawTimingRow from(RequestTimingSnapshot snapshot, String operation, boolean warmup) {
            double repositoryMillis = snapshot.operations().stream()
                    .mapToDouble(CrudOperationTiming::elapsedMillis)
                    .sum();

            CrudOperationTiming dominantOperation = snapshot.operations().stream()
                    .max(Comparator.comparingDouble(CrudOperationTiming::elapsedMillis))
                    .orElse(null);

            double overheadMillis = Math.max(0.0, snapshot.totalElapsedMillis() - repositoryMillis);
            return new RawTimingRow(
                    snapshot.timestamp().toString(),
                    warmup ? "warmup" : "measured",
                    operation,
                    snapshot.iteration() == null ? "" : Integer.toString(snapshot.iteration()),
                    String.format(Locale.US, "%.3f", snapshot.totalElapsedMillis()),
                    String.format(Locale.US, "%.3f", repositoryMillis),
                    String.format(Locale.US, "%.3f", overheadMillis),
                    dominantOperation == null ? "" : dominantOperation.operationName(),
                    dominantOperation == null ? "0.000" : String.format(Locale.US, "%.3f", dominantOperation.elapsedMillis()),
                    Integer.toString(snapshot.httpStatus()),
                    snapshot.error() == null ? "" : snapshot.error()
            );
        }

        private String toCsvLine() {
            return String.join(",",
                    escape(timestamp),
                    escape(phase),
                    escape(operation),
                    escape(iteration),
                    escape(totalMillis),
                    escape(repositoryMillis),
                    escape(overheadMillis),
                    escape(dominantRepositoryOperation),
                    escape(dominantRepositoryMillis),
                    escape(httpStatus),
                    escape(error)
            );
        }
    }
}
