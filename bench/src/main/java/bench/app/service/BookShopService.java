package bench.app.service;

import bench.app.model.common.Book;
import bench.app.model.common.BookShop;

import java.util.List;

public interface BookShopService<Id> {
    List<BookShop> getBookShops();
    List<Book> getBooks(Id bookShopId, boolean onlyAvailable);
}
