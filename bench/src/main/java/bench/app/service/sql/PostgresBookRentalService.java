package bench.app.service.sql;

import bench.app.model.common.BookRental;
import bench.app.repository.sql.postgres.PostgresBookRentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PostgresBookRentalService {
    private final PostgresBookRentalRepository bookRentalRepository;

    public PostgresBookRentalService(PostgresBookRentalRepository bookRentalRepository) {
        this.bookRentalRepository = bookRentalRepository;
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<BookRental> getBookRentalsByShopId(long shopId) {
        return bookRentalRepository.findByBook_BookShop_Id((int) shopId)
            .stream()
            .map(br -> br.convertToModel())
            .toList();
    }
}
