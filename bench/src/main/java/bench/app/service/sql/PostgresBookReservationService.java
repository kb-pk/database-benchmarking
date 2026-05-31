package bench.app.service.sql;

import bench.app.model.sql.BookReservation;
import bench.app.repository.sql.postgres.PostgresBookReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostgresBookReservationService {
    private final PostgresBookReservationRepository bookReservationRepository;

    public PostgresBookReservationService(PostgresBookReservationRepository bookReservationRepository) {
        this.bookReservationRepository = bookReservationRepository;
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<bench.app.model.common.BookReservation> getBookReservationsByShopId(long shopId) {
        return bookReservationRepository.findByBook_BookShop_Id((int) shopId)
                .stream()
                .map(BookReservation::convertToModel)
                .toList();
    }
}
