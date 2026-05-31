package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BookShopOfferingSnapshotStore {
    private final Map<String, List<BookShopOfferingSnapshot>> snapshots = new ConcurrentHashMap<>();

    public void save(String dbEngine, long userId, List<BookShopOfferingSnapshot> rows) {
        snapshots.put(key(dbEngine, userId), List.copyOf(rows));
    }

    public Optional<List<BookShopOfferingSnapshot>> find(String dbEngine, long userId) {
        return Optional.ofNullable(snapshots.get(key(dbEngine, userId)));
    }

    public void remove(String dbEngine, long userId) {
        snapshots.remove(key(dbEngine, userId));
    }

    private String key(String dbEngine, long userId) {
        return dbEngine + ":" + userId;
    }
}