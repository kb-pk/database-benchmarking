package bench.app.service.sql;

import bench.app.model.common.BookShop;
import bench.app.model.common.Book;
import bench.app.repository.sql.mssql.MssqlBookShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MssqlBookShopService extends SQLBookShopService<MssqlBookShopRepository> {
    private final MssqlBookShopRepository bookShopRepository;

    public MssqlBookShopService(MssqlBookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public MssqlBookShopRepository getRepository() {
        return this.bookShopRepository;
    }

    @Override
    @Transactional(transactionManager = "mssqlTransactionManager", readOnly = true)
    public List<BookShop> getBookShops() {
        return super.getBookShops();
    }

    @Override
    @Transactional(transactionManager = "mssqlTransactionManager", readOnly = true)
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        return super.getBooks(bookShopId, onlyAvailable);
    }
}
