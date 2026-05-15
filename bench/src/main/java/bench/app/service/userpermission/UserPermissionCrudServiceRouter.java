package bench.app.service.userpermission;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class UserPermissionCrudServiceRouter implements UserPermissionCrudOperations {
    private final DatabaseEngine activeEngine;
    private final UserPermissionCrudEngineService delegate;

    public UserPermissionCrudServiceRouter(
            BenchmarkEngineResolver engineResolver,
            List<UserPermissionCrudEngineService> services
    ) {
        this.activeEngine = engineResolver.resolveEngine();
        this.delegate = services.stream()
                .filter(service -> service.supports(activeEngine))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Brak serwisu CRUD dla silnika " + activeEngine.propertyValue()
                ));
    }

    @Override
    public Map<String, Object> create(Integer requestedId, String permission, String details) {
        return delegate.create(activeEngine, requestedId, permission, details);
    }

    @Override
    public Map<String, Object> read(int id) {
        return delegate.read(activeEngine, id);
    }

    @Override
    public Map<String, Object> update(int id, String permission, String details) {
        return delegate.update(activeEngine, id, permission, details);
    }

    @Override
    public Map<String, Object> delete(int id) {
        return delegate.delete(activeEngine, id);
    }
}
