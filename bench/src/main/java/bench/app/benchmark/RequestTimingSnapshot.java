package bench.app.benchmark;

import java.time.Instant;
import java.util.List;

public record RequestTimingSnapshot(
        Instant timestamp,
        String requestId,
        String httpMethod,
        String path,
        String operationLabel,
        Integer iteration,
        String dbEngine,
        int httpStatus,
        double totalElapsedMillis,
        List<CrudOperationTiming> operations,
        String error
) {
}
