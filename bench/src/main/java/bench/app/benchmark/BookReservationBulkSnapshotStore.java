package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookReservationBulkSnapshotStore {
    private final Map<String, List<BookReservationBulkSnapshot>> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, int monthsThreshold, List<BookReservationBulkSnapshot> snapshotRows) {
        snapshots.put(key(dbEngine, monthsThreshold), List.copyOf(snapshotRows));
    }

    public Optional<List<BookReservationBulkSnapshot>> find(String dbEngine, int monthsThreshold) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, monthsThreshold)));
    }

    public void remove(String dbEngine, int monthsThreshold) {
        snapshots.remove(key(dbEngine, monthsThreshold));
    }

    private String key(String dbEngine, int monthsThreshold) {
        return dbEngine + ":" + monthsThreshold;
    }
}