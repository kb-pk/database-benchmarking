package bench.app.util;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public class CsvCrudLogger {
    public enum CrudType { CREATE, READ, UPDATE, DELETE }

    private final String fileBaseName;
    private final Map<CrudType, CSVPrinter> printers = new EnumMap<>(CrudType.class);

    public CsvCrudLogger(String engine, String size) throws IOException {
        // Mapuj engine name na folder
        String folderName = mapEngineToPreciseFolder(engine);
        Path outputDir = Paths.get("../output_data", folderName);
        Files.createDirectories(outputDir);
        
        this.fileBaseName = outputDir.resolve(engine + "_" + size).toString();
        for (CrudType type : CrudType.values()) {
            FileWriter writer = new FileWriter(this.fileBaseName + "_" + type.name().toLowerCase() + ".csv", true);
            CSVFormat format;
            if (type == CrudType.CREATE) {
                format = CSVFormat.DEFAULT.withHeader(
                        "operation_number",
                        "start_time_1", "end_time_1", "duration_ms_1",
                        "start_time_2", "end_time_2", "duration_ms_2",
                        "start_time_3", "end_time_3", "duration_ms_3",
                        "average_ms"
                );
            } else {
                format = CSVFormat.DEFAULT.withHeader("operation_number", "start_time", "end_time", "duration_ms");
            }
            CSVPrinter printer = new CSVPrinter(writer, format);
            printers.put(type, printer);
        }
    }

    public void log(CrudType type, int operationNumber, Instant start, Instant end) throws IOException {
        long duration = end.toEpochMilli() - start.toEpochMilli();
        CSVPrinter printer = printers.get(type);
        printer.printRecord(operationNumber, start.toString(), end.toString(), duration);
        printer.flush();
    }

    public void logCreate(int operationNumber, Instant start1, Instant end1, Instant start2, Instant end2, Instant start3, Instant end3, double averageDurationMs) throws IOException {
        CSVPrinter printer = printers.get(CrudType.CREATE);
        printer.printRecord(
                operationNumber,
                start1.toString(), end1.toString(), formatDurationMs(start1, end1),
                start2.toString(), end2.toString(), formatDurationMs(start2, end2),
                start3.toString(), end3.toString(), formatDurationMs(start3, end3),
                String.format(Locale.US, "%.4f", averageDurationMs)
        );
        printer.flush();
    }

    private String formatDurationMs(Instant start, Instant end) {
        double durationMs = Duration.between(start, end).toNanos() / 1_000_000.0;
        return String.format(Locale.US, "%.4f", durationMs);
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

    public void close() throws IOException {
        for (CSVPrinter printer : printers.values()) {
            printer.close();
        }
    }
}
