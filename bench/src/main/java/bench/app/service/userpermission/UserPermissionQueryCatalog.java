package bench.app.service.userpermission;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class UserPermissionQueryCatalog {
    private final Map<DatabaseEngine, Map<UserPermissionQueryType, String>> predefinedQueries;

    public UserPermissionQueryCatalog() {
        this.predefinedQueries = new EnumMap<>(DatabaseEngine.class);

        Map<UserPermissionQueryType, String> relationalQueries = new EnumMap<>(UserPermissionQueryType.class);
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.useraccountpermissions");
        relationalQueries.put(UserPermissionQueryType.CREATE,
                "INSERT INTO bench.useraccountpermissions (id, permission, details) VALUES (?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.READ,
                "SELECT id, permission, details FROM bench.useraccountpermissions WHERE id = ?");
        relationalQueries.put(UserPermissionQueryType.UPDATE,
                "UPDATE bench.useraccountpermissions SET permission = ?, details = ? WHERE id = ?");
        relationalQueries.put(UserPermissionQueryType.DELETE,
                "DELETE FROM bench.useraccountpermissions WHERE id = ?");

        predefinedQueries.put(DatabaseEngine.POSTGRESQL, relationalQueries);
        predefinedQueries.put(DatabaseEngine.MSSQL, relationalQueries);
        predefinedQueries.put(DatabaseEngine.CASSANDRA, Map.of());
        predefinedQueries.put(DatabaseEngine.SCYLLA, Map.of());
    }

    public String getRequiredQuery(DatabaseEngine engine, UserPermissionQueryType queryType) {
        Map<UserPermissionQueryType, String> engineQueries = predefinedQueries.get(engine);
        if (engineQueries == null || !engineQueries.containsKey(queryType)) {
            throw new IllegalArgumentException(
                    "Brak predefiniowanego zapytania " + queryType + " dla silnika " + engine.propertyValue()
            );
        }
        return engineQueries.get(queryType);
    }
}
