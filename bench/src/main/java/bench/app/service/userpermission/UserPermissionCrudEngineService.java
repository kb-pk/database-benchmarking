package bench.app.service.userpermission;

import java.util.Map;

public interface UserPermissionCrudEngineService {
    boolean supports(DatabaseEngine engine);

    Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details);

    Map<String, Object> read(DatabaseEngine engine, int id);

    Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details);

    Map<String, Object> delete(DatabaseEngine engine, int id);
}
