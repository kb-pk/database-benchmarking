package bench.service;

import bench.model.common.Book;

import java.util.List;

public class CassandraBookShopService implements BookShopService {

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new  UnsupportedOperationException();
    }
}
