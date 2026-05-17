package bench.app.service.cql;

import bench.app.model.cassandra.BookShop;
import bench.app.model.common.Book;
import bench.app.service.BookShopService;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.List;
import java.util.UUID;

public abstract class CQLBookShopService<Repo extends CassandraRepository<BookShop, UUID>>
        implements BookShopService<UUID> {
    public abstract Repo getRepository();

    @Override
    public List<bench.app.model.common.BookShop> getBookShops() {
        return List.of();
    }

    @Override
    public List<Book> getBooks(UUID bookShopId, boolean onlyAvailable) {
        return List.of();
    }
}
