package bench.app.benchmark;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class RequestTimingContext {
    private final Instant timestamp;
    private final String requestId;
    private final String httpMethod;
    private final String path;
    private final String operationLabel;
    private final Integer iteration;
    private final Integer warmupIterations;
    private final String dbEngine;
    private final long requestStartNanos;
    private long excludedNanos;
    private final List<CrudOperationTiming> operations = new ArrayList<>();

    public RequestTimingContext(
            String requestId,
            String httpMethod,
            String path,
            String operationLabel,
            Integer iteration,
            Integer warmupIterations,
            String dbEngine
    ) {
        this.timestamp = Instant.now();
        this.requestId = requestId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.operationLabel = operationLabel;
        this.iteration = iteration;
        this.warmupIterations = warmupIterations;
        this.dbEngine = dbEngine;
        this.requestStartNanos = System.nanoTime();
    }

    public void addOperation(CrudOperationTiming operation) {
        this.operations.add(operation);
    }

    public void addExcludedNanos(long excludedNanos) {
        if (excludedNanos > 0) {
            this.excludedNanos += excludedNanos;
        }
    }

    public RequestTimingSnapshot toSnapshot(int status, String error) {
        long elapsedNanos = System.nanoTime() - this.requestStartNanos - this.excludedNanos;
        double elapsedMillis = elapsedNanos / 1_000_000.0;
        return new RequestTimingSnapshot(
                this.timestamp,
                this.requestId,
                this.httpMethod,
                this.path,
                this.operationLabel,
                this.iteration,
                this.warmupIterations,
                this.dbEngine,
                status,
                elapsedMillis,
                List.copyOf(this.operations),
                error
        );
    }
}
