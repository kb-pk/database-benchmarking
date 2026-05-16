package bench.app.service;

import bench.app.model.common.Book;

import java.util.List;

public interface BookShopService<Id> {
    List<Book> getBooks(Id bookShopId, boolean onlyAvailable);
}
