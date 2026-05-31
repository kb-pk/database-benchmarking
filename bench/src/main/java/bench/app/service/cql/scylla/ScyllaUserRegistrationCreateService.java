package bench.app.service.cql.scylla;

import bench.app.model.common.UserRegistrationCreateResult;
import bench.app.service.cql.cassandra.CassandraUserRegistrationCreateService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ScyllaUserRegistrationCreateService {
    private final CassandraUserRegistrationCreateService delegate;

    public ScyllaUserRegistrationCreateService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraUserRegistrationCreateService(scyllaSession);
    }

    public UserRegistrationCreateResult createUserRegistration(
            String name,
            String surname,
            String phoneNumber,
            String email,
            String login,
            String passwordHash,
            boolean restoreAfterCreate
    ) {
        return this.delegate.createUserRegistration(name, surname, phoneNumber, email, login, passwordHash, restoreAfterCreate);
    }
}