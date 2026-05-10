package bench.service;

import bench.model.common.Book;
import bench.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SQLBookShopService implements BookShopService {
    @Autowired
    private BookRepository bookRepository;

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
