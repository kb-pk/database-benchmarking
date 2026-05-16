package bench.app.service;

import bench.app.model.common.Book;
import bench.app.repository.cassandra.BookShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CassandraBookShopService implements BookShopService<UUID> {
    private final BookShopRepository bookShopRepository;

    public CassandraBookShopService(BookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public List<Book> getBooks(UUID bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
