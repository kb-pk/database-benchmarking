package bench.app.service.cql.scylla;

import bench.app.model.common.BookRentalConditionalCreateResult;
import bench.app.service.cql.cassandra.CassandraBookRentalConditionalCreateService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ScyllaBookRentalConditionalCreateService {
    private final CassandraBookRentalConditionalCreateService delegate;

    public ScyllaBookRentalConditionalCreateService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraBookRentalConditionalCreateService(scyllaSession);
    }

    public BookRentalConditionalCreateResult createRentalIfValid(
            UUID shopId,
            UUID bookId,
            UUID userId,
            LocalDate startDate,
            boolean restoreAfterCreate
    ) {
        return this.delegate.createRentalIfValid(shopId, bookId, userId, startDate, restoreAfterCreate);
    }
}