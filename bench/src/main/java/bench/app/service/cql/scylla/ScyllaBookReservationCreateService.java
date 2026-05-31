package bench.app.service.cql.scylla;

import bench.app.model.common.BookReservationCreateResult;
import bench.app.service.cql.cassandra.CassandraBookReservationCreateService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ScyllaBookReservationCreateService {
    private final CassandraBookReservationCreateService delegate;

    public ScyllaBookReservationCreateService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookReservationCreateService(scyllaSession);
    }

    public BookReservationCreateResult createReservation(UUID bookId, UUID userId, LocalDate whenReserved, boolean restoreAfterCreate) {
        return this.delegate.createReservation(bookId, userId, whenReserved, restoreAfterCreate);
    }
}