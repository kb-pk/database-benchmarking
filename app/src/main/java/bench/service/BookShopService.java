package bench.service;

import bench.model.common.Book;

import java.util.List;

public interface BookShopService {
    List<Book> getBooks(Long bookShopId, boolean onlyAvailable);
}
