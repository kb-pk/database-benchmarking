package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

@Component
public class RequestTimingContextHolder {
    private static final ThreadLocal<RequestTimingContext> CONTEXT = new ThreadLocal<>();

    public void start(
            String requestId,
            String httpMethod,
            String path,
            String operationLabel,
            Integer iteration,
            Integer warmupIterations,
            String dbEngine
    ) {
        CONTEXT.set(new RequestTimingContext(requestId, httpMethod, path, operationLabel, iteration, warmupIterations, dbEngine));
    }

    public Optional<RequestTimingContext> getCurrent() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public <T> T excludeFromTiming(Supplier<T> supplier) {
        long startNanos = System.nanoTime();
        try {
            return supplier.get();
        } finally {
            RequestTimingContext context = CONTEXT.get();
            if (context != null) {
                context.addExcludedNanos(System.nanoTime() - startNanos);
            }
        }
    }

    public void excludeFromTiming(Runnable runnable) {
        excludeFromTiming(() -> {
            runnable.run();
            return null;
        });
    }

    public void clear() {
        CONTEXT.remove();
    }
}
