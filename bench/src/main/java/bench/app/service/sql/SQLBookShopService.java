package bench.app.service.sql;

import bench.app.model.common.Book;
import bench.app.model.common.BookShop;
import bench.app.service.BookShopService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public abstract class SQLBookShopService<Repo extends JpaRepository<bench.app.model.sql.BookShop, Integer>>
        implements BookShopService<Long> {
    public abstract Repo getRepository();

    @Override
    public List<BookShop> getBookShops() {
        return this.getRepository().findAll().stream()
                .map(bench.app.model.sql.BookShop::convertToModel)
                .toList();
    }

    @Override
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
