package bench.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import bench.app.util.CsvCrudLogger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class UserController {
	private static final int CREATE_REPETITIONS = 3;
	private final JdbcTemplate jdbcTemplate;
	// Logger CSV dla CREATE (można rozbudować o parametry silnik/rozmiar)
	private final CsvCrudLogger csvLogger;
	private int createOperationCounter = 1;
	private static final Pattern LOAD_SQL_PATTERN = Pattern.compile("inserts_([a-zA-Z0-9]+)_([0-9]+)\\.sql");

	public UserController(
			JdbcTemplate jdbcTemplate,
			@Value("${bench.load-sql:inserts_postgresql_10000.sql}") String loadSqlFile
	) {
		this.jdbcTemplate = jdbcTemplate;

		String engine = "postgresql";
		String size = "10000";
		String fileName = Paths.get(loadSqlFile).getFileName().toString();
		Matcher matcher = LOAD_SQL_PATTERN.matcher(fileName);
		if (matcher.matches()) {
			engine = matcher.group(1);
			size = matcher.group(2);
		}

		CsvCrudLogger logger = null;
		try {
			logger = new CsvCrudLogger(engine, size);
		} catch (IOException e) {
			// Możesz zalogować błąd lub rzucić wyjątek
			System.err.println("Nie można utworzyć loggera CSV: " + e.getMessage());
		}
		this.csvLogger = logger;
	}

	@PostMapping("/sql/user-account-permissions")
	public Map<String, Object> createUserAccountPermission(@RequestBody CreateUserAccountPermissionRequest request) {
		if (request.permission() == null || request.permission().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "permission is required");
		}

		Integer maxId = jdbcTemplate.queryForObject(
				"SELECT COALESCE(MAX(id), 0) FROM bench.useraccountpermissions",
				Integer.class
		);
		int nextId = (maxId == null ? 0 : maxId) + 1;
		int firstIdToInsert = (request.id() == null || request.id() <= 0) ? nextId : request.id();
		List<Integer> insertedIds = new ArrayList<>();
		List<Instant> starts = new ArrayList<>();
		List<Instant> ends = new ArrayList<>();
		double totalDurationMs = 0.0;

		try {
			for (int i = 0; i < CREATE_REPETITIONS; i++) {
				int currentId = firstIdToInsert + i;
				Instant start = Instant.now();
				int affectedRows = jdbcTemplate.update(
						"INSERT INTO bench.useraccountpermissions (id, permission, details) VALUES (?, ?, ?)",
						currentId,
						request.permission(),
						request.details()
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

				System.out.println("[INFO] CREATE operation executed (" + (i + 1) + "/" + CREATE_REPETITIONS + "): ID=" + currentId + ", Permission=" + request.permission() + ", Details=" + request.details());
			}

			double averageDurationMs = totalDurationMs / CREATE_REPETITIONS;
			
			// Logowanie wszystkich 3 czasow + srednia w jednym wierszu CSV
			if (csvLogger != null) {
				try {
					csvLogger.logCreate(
							createOperationCounter++,
							starts.get(0), ends.get(0),
							starts.get(1), ends.get(1),
							starts.get(2), ends.get(2),
							averageDurationMs
					);
				} catch (IOException e) {
					System.err.println("Błąd logowania do CSV: " + e.getMessage());
				}
			}
			System.out.println("[INFO] CREATE average duration for " + CREATE_REPETITIONS + " runs: " + String.format(Locale.US, "%.4f", averageDurationMs) + " ms");

			return Map.of(
					"status", "created",
					"executions", CREATE_REPETITIONS,
					"ids", insertedIds,
					"average_duration_ms", String.format(Locale.US, "%.4f", averageDurationMs),
					"permission", request.permission()
			);
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cannot create permission: " + ex.getMessage());
		}
	}

	public record CreateUserAccountPermissionRequest(Integer id, String permission, String details) {
	}
}
