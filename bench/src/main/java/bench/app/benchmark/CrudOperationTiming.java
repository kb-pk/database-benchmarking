package bench.app.benchmark;

public record CrudOperationTiming(
        CrudOperationType operationType,
        String operationName,
        double elapsedMillis
) {
}
