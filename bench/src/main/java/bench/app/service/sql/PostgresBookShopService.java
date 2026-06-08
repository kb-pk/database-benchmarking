package bench.app.service.sql;

import bench.app.model.common.Book;
import bench.app.model.common.BookShop;
import bench.app.repository.sql.postgres.PostgresBookShopRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Service
public class PostgresBookShopService extends SQLBookShopService<PostgresBookShopRepository> {
    private static final String SELECT_BOOKS_BY_SHOP = """
        SELECT b.author, b.title, b.publisher, b.publishdate, b.pages, b.isinreadingroom,
           s.shopname, s.address, s.email
        FROM bench.book b
        JOIN bench.bookshop s ON s.id = b.bookshopid
        WHERE b.bookshopid = ?
        ORDER BY b.id
        """;

    private static final String SELECT_AVAILABLE_BOOKS_BY_SHOP = """
        SELECT b.author, b.title, b.publisher, b.publishdate, b.pages, b.isinreadingroom,
           s.shopname, s.address, s.email
        FROM bench.book b
        JOIN bench.bookshop s ON s.id = b.bookshopid
        WHERE b.bookshopid = ?
          AND b.isinreadingroom = 0
        ORDER BY b.id
        """;

    private final PostgresBookShopRepository bookShopRepository;
    private final JdbcTemplate jdbcTemplate;

    public PostgresBookShopService(
        PostgresBookShopRepository bookShopRepository,
        @Qualifier("postgresDataSource") DataSource dataSource
    ) {
        this.bookShopRepository = bookShopRepository;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public PostgresBookShopRepository getRepository() {
        return this.bookShopRepository;
    }

    @Override
    @Transactional(transactionManager = "postgresTransactionManager", readOnly = true)
    public List<BookShop> getBookShops() {
        return super.getBookShops();
    }

    @Override
    @Transactional(transactionManager = "postgresTransactionManager", readOnly = true)
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        String sql = onlyAvailable ? SELECT_AVAILABLE_BOOKS_BY_SHOP : SELECT_BOOKS_BY_SHOP;
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    BookShop shop = new BookShop(
                            null,
                            null,
                            Collections.emptyList(),
                            rs.getString("shopname"),
                            rs.getString("address"),
                            rs.getString("email")
                    );

                    return new Book(
                            shop,
                            rs.getString("author"),
                            rs.getString("title"),
                            rs.getString("publisher"),
                            rs.getDate("publishdate"),
                            rs.getInt("pages"),
                            rs.getBoolean("isinreadingroom")
                    );
                },
                bookShopId
        );
    }
}
