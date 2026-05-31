package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EmployeeRentalDaySnapshotStore {
    private final Map<String, List<EmployeeRentalDaySnapshot>> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, long employeeId, LocalDate rentalDate, List<EmployeeRentalDaySnapshot> rows) {
        snapshots.put(key(dbEngine, employeeId, rentalDate), List.copyOf(rows));
    }

    public Optional<List<EmployeeRentalDaySnapshot>> find(String dbEngine, long employeeId, LocalDate rentalDate) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, employeeId, rentalDate)));
    }

    public void remove(String dbEngine, long employeeId, LocalDate rentalDate) {
        snapshots.remove(key(dbEngine, employeeId, rentalDate));
    }

    private String key(String dbEngine, long employeeId, LocalDate rentalDate) {
        return dbEngine + ":" + employeeId + ":" + rentalDate;
    }
}