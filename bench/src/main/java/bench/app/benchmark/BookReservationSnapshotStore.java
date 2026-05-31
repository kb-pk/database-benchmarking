package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookReservationSnapshotStore {
    private final Map<String, BookReservationSnapshot> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, long reservationId, BookReservationSnapshot snapshot) {
        snapshots.put(key(dbEngine, reservationId), snapshot);
    }

    public Optional<BookReservationSnapshot> find(String dbEngine, long reservationId) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, reservationId)));
    }

    public void remove(String dbEngine, long reservationId) {
        snapshots.remove(key(dbEngine, reservationId));
    }

    public void removeIfMatches(String dbEngine, long reservationId, BookReservationSnapshot snapshot) {
        snapshots.remove(key(dbEngine, reservationId), snapshot);
    }

    private String key(String dbEngine, long reservationId) {
        return dbEngine + ":" + reservationId;
    }
}