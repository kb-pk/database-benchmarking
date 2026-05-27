package bench.app.benchmark;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RequestTimingContextHolder {
    private static final ThreadLocal<RequestTimingContext> CONTEXT = new ThreadLocal<>();

    public void start(String requestId, String httpMethod, String path, String dbEngine) {
        CONTEXT.set(new RequestTimingContext(requestId, httpMethod, path, dbEngine));
    }

    public Optional<RequestTimingContext> getCurrent() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public void clear() {
        CONTEXT.remove();
    }
}
