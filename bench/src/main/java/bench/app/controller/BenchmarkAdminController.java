package bench.app.controller;

import bench.app.benchmark.CsvTimingLogWriter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/benchmark")
public class BenchmarkAdminController {
    private final CsvTimingLogWriter csvTimingLogWriter;

    public BenchmarkAdminController(CsvTimingLogWriter csvTimingLogWriter) {
        this.csvTimingLogWriter = csvTimingLogWriter;
    }

    // Endpoint do czyszczenia pliku CSV dla wybranego silnika
    @PostMapping("/clear-csv")
    public String clearCsv(@RequestParam String db) {
        csvTimingLogWriter.clearCsvForEngine(db);
        return "Wyczyszczono plik CSV dla silnika: " + db;
    }
}
