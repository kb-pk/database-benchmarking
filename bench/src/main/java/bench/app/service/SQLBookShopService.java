package bench.app.service;

import bench.app.model.common.Book;
import bench.app.repository.sql.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SQLBookShopService implements BookShopService {
    private final BookRepository bookRepository;

    public SQLBookShopService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
