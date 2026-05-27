package bench.app.benchmark;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class CrudTimingAspect {
    private final RequestTimingContextHolder contextHolder;
    private final CrudMethodClassifier methodClassifier;

    public CrudTimingAspect(RequestTimingContextHolder contextHolder, CrudMethodClassifier methodClassifier) {
        this.contextHolder = contextHolder;
        this.methodClassifier = methodClassifier;
    }

    @Around("execution(* bench.app.repository..*.*(..))")
    public Object measureRepositoryCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (contextHolder.getCurrent().isEmpty()) {
            return joinPoint.proceed();
        }

        long startNanos = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedMillis = elapsedNanos / 1_000_000.0;

            String methodName = joinPoint.getSignature().getName();
            CrudOperationType operationType = methodClassifier.classify(methodName);

            String repositoryClass = joinPoint.getSignature().getDeclaringType().getSimpleName();
            String operationName = repositoryClass + "." + methodName;

            contextHolder.getCurrent().ifPresent(context ->
                    context.addOperation(new CrudOperationTiming(operationType, operationName, elapsedMillis))
            );
        }
    }
}
