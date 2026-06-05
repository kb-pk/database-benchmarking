package bench.app.benchmark;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

@Component
public class RequestTimingInterceptor implements HandlerInterceptor {
    private final RequestTimingContextHolder contextHolder;
    private final CsvTimingLogWriter csvTimingLogWriter;

    public RequestTimingInterceptor(RequestTimingContextHolder contextHolder, CsvTimingLogWriter csvTimingLogWriter) {
        this.contextHolder = contextHolder;
        this.csvTimingLogWriter = csvTimingLogWriter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (isSkipTimingRequest(request)) {
            return true;
        }

        String requestId = UUID.randomUUID().toString();
        String dbEngine = request.getParameter("db");
        String operationLabel = resolveOperationLabel(request);
        Integer iteration = parseIteration(request.getParameter("iteration"));
        Integer warmupIterations = parsePositiveInt(request.getParameter("warmupIterations"));

        contextHolder.start(requestId, request.getMethod(), request.getRequestURI(), operationLabel, iteration, warmupIterations, dbEngine);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception exception) {
        try {
            if (isSkipTimingRequest(request)) {
                return;
            }

            String path = request.getRequestURI();
            // Nie loguj czyszczenia CSV
            if ("/benchmark/clear-csv".equals(path)) {
                return;
            }
            contextHolder.getCurrent().ifPresent(context -> {
                String error = exception != null ? exception.getClass().getSimpleName() + ": " + exception.getMessage() : "";
                RequestTimingSnapshot snapshot = context.toSnapshot(response.getStatus(), error);
                csvTimingLogWriter.append(snapshot);
            });
        } finally {
            contextHolder.clear();
        }
    }

    private String resolveOperationLabel(HttpServletRequest request) {
        String operationLabel = request.getParameter("operation");
        if (operationLabel == null || operationLabel.isBlank()) {
            operationLabel = request.getParameter("op");
        }
        if (operationLabel == null || operationLabel.isBlank()) {
            return request.getMethod() + " " + request.getRequestURI();
        }
        return operationLabel.trim();
    }

    private Integer parseIteration(String rawIteration) {
        return parsePositiveInt(rawIteration);
    }

    private Integer parsePositiveInt(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(rawValue.trim());
            if (parsed >= 1) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return null;
    }

    private boolean isSkipTimingRequest(HttpServletRequest request) {
        return "true".equalsIgnoreCase(request.getParameter("skipBenchmarkTiming"));
    }
}
