package bench.app.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RequestTimingContext {
    private final Instant timestamp;
    private final String requestId;
    private final String httpMethod;
    private final String path;
    private final String dbEngine;
    private final long requestStartNanos;
    private final List<CrudOperationTiming> operations = new ArrayList<>();

    public RequestTimingContext(String requestId, String httpMethod, String path, String dbEngine) {
        this.timestamp = Instant.now();
        this.requestId = requestId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.dbEngine = dbEngine;
        this.requestStartNanos = System.nanoTime();
    }

    public void addOperation(CrudOperationTiming operation) {
        this.operations.add(operation);
    }

    public RequestTimingSnapshot toSnapshot(int status, String error) {
        long elapsedNanos = System.nanoTime() - this.requestStartNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        return new RequestTimingSnapshot(
                this.timestamp,
                this.requestId,
                this.httpMethod,
                this.path,
                this.dbEngine,
                status,
                elapsedMillis,
                List.copyOf(this.operations),
                error
        );
    }
}
