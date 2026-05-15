package bench.app.service.userpermission;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class WideColumnUserPermissionCrudService implements UserPermissionCrudEngineService {
    @Override
    public boolean supports(DatabaseEngine engine) {
        return engine == DatabaseEngine.CASSANDRA || engine == DatabaseEngine.SCYLLA;
    }

    @Override
    public Map<String, Object> create(DatabaseEngine engine, Integer requestedId, String permission, String details) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> read(DatabaseEngine engine, int id) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> update(DatabaseEngine engine, int id, String permission, String details) {
        throw unsupported(engine);
    }

    @Override
    public Map<String, Object> delete(DatabaseEngine engine, int id) {
        throw unsupported(engine);
    }

    private ResponseStatusException unsupported(DatabaseEngine engine) {
        return new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "CRUD useraccountpermissions nie jest jeszcze zaimplementowany dla silnika " + engine.propertyValue()
        );
    }
}
