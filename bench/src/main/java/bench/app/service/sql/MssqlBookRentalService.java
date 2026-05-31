package bench.app.service.sql;

import bench.app.model.common.BookRental;
import bench.app.repository.sql.mssql.MssqlBookRentalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MssqlBookRentalService {
    private final MssqlBookRentalRepository bookRentalRepository;

    public MssqlBookRentalService(MssqlBookRentalRepository bookRentalRepository) {
        this.bookRentalRepository = bookRentalRepository;
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<BookRental> getBookRentalsByShopId(long shopId) {
        return bookRentalRepository.findByBook_BookShop_Id((int) shopId)
            .stream()
            .map(br -> br.convertToModel())
            .toList();
    }
}
