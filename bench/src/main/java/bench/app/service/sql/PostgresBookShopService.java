package bench.app.service.sql;

import bench.app.model.common.Book;
import bench.app.model.common.BookShop;
import bench.app.repository.sql.postgres.PostgresBookShopRepository;
import bench.app.service.BookShopService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostgresBookShopService extends SQLBookShopService<PostgresBookShopRepository> {
    private final PostgresBookShopRepository bookShopRepository;

    public PostgresBookShopService(PostgresBookShopRepository bookShopRepository) {
        this.bookShopRepository = bookShopRepository;
    }

    @Override
    public PostgresBookShopRepository getRepository() {
        return this.bookShopRepository;
    }
}
