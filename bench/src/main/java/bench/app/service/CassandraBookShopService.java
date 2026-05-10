package bench.app.service;

import bench.app.model.common.Book;
import bench.app.repository.cassandra.BookShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CassandraBookShopService implements BookShopService {
    private final BookShopRepository bookShopRepository;

    public CassandraBookShopService(BookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
