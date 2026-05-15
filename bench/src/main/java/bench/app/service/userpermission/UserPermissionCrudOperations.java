package bench.app.service.userpermission;

import java.util.Map;

public interface UserPermissionCrudOperations {
    Map<String, Object> create(Integer requestedId, String permission, String details);

    Map<String, Object> read(int id);

    Map<String, Object> update(int id, String permission, String details);

    Map<String, Object> delete(int id);
}
