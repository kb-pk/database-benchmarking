package bench.app.service;

import bench.app.model.common.Book;
import bench.app.repository.sql.postgres.PostgresBookShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SQLBookShopService implements BookShopService<Long> {
    private final PostgresBookShopRepository bookShopRepository;

    public SQLBookShopService(PostgresBookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
