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
        String requestId = UUID.randomUUID().toString();
        String dbEngine = request.getParameter("db");

        contextHolder.start(requestId, request.getMethod(), request.getRequestURI(), dbEngine);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception exception) {
        try {
            contextHolder.getCurrent().ifPresent(context -> {
                String error = exception != null ? exception.getClass().getSimpleName() + ": " + exception.getMessage() : "";
                RequestTimingSnapshot snapshot = context.toSnapshot(response.getStatus(), error);
                csvTimingLogWriter.append(snapshot);
            });
        } finally {
            contextHolder.clear();
        }
    }
}
