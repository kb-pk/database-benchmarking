package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookRentalSnapshotStore {
    private final Map<String, BookRentalSnapshot> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, long rentalId, BookRentalSnapshot snapshot) {
        snapshots.put(key(dbEngine, rentalId), snapshot);
    }

    public Optional<BookRentalSnapshot> find(String dbEngine, long rentalId) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, rentalId)));
    }

    public void removeIfMatches(String dbEngine, long rentalId, BookRentalSnapshot snapshot) {
        snapshots.remove(key(dbEngine, rentalId), snapshot);
    }

    private String key(String dbEngine, long rentalId) {
        return dbEngine + ":" + rentalId;
    }
}