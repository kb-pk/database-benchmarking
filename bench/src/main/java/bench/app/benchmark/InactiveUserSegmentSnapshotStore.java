package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InactiveUserSegmentSnapshotStore {
    private final Map<String, InactiveUserSegmentSnapshot> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, int monthsThreshold, int segmentSize, InactiveUserSegmentSnapshot snapshot) {
        snapshots.put(key(dbEngine, monthsThreshold, segmentSize), snapshot);
    }

    public Optional<InactiveUserSegmentSnapshot> find(String dbEngine, int monthsThreshold, int segmentSize) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, monthsThreshold, segmentSize)));
    }

    public void remove(String dbEngine, int monthsThreshold, int segmentSize) {
        snapshots.remove(key(dbEngine, monthsThreshold, segmentSize));
    }

    private String key(String dbEngine, int monthsThreshold, int segmentSize) {
        return dbEngine + ":" + monthsThreshold + ":" + segmentSize;
    }
}