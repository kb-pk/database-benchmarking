package bench.app.service.sql;

import bench.app.model.common.BookShop;
import bench.app.model.common.Book;
import bench.app.repository.sql.mssql.MssqlBookShopRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Service
public class MssqlBookShopService extends SQLBookShopService<MssqlBookShopRepository> {
    private static final String SELECT_BOOKS_BY_SHOP = """
        SELECT b.author, b.title, b.publisher, b.publishDate, b.pages, b.isInReadingRoom,
           s.shopName, s.address, s.email
        FROM bench.Book b
        JOIN bench.BookShop s ON s.id = b.bookShopId
        WHERE b.bookShopId = ?
        ORDER BY b.id
        """;

    private static final String SELECT_AVAILABLE_BOOKS_BY_SHOP = """
        SELECT b.author, b.title, b.publisher, b.publishDate, b.pages, b.isInReadingRoom,
           s.shopName, s.address, s.email
        FROM bench.Book b
        JOIN bench.BookShop s ON s.id = b.bookShopId
        WHERE b.bookShopId = ?
          AND b.isInReadingRoom = 0
        ORDER BY b.id
        """;

    private final MssqlBookShopRepository bookShopRepository;
    private final JdbcTemplate jdbcTemplate;

    public MssqlBookShopService(
        MssqlBookShopRepository bookShopRepository,
        @Qualifier("mssqlDataSource") DataSource dataSource
    ) {
        this.bookShopRepository = bookShopRepository;
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public MssqlBookShopRepository getRepository() {
        return this.bookShopRepository;
    }

    @Override
    @Transactional(transactionManager = "mssqlTransactionManager", readOnly = true)
    public List<BookShop> getBookShops() {
        return super.getBookShops();
    }

    @Override
    @Transactional(transactionManager = "mssqlTransactionManager", readOnly = true)
    public List<Book> getBooks(Long bookShopId, boolean onlyAvailable) {
        String sql = onlyAvailable ? SELECT_AVAILABLE_BOOKS_BY_SHOP : SELECT_BOOKS_BY_SHOP;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> {
                BookShop shop = new BookShop(
                    null,
                    null,
                    Collections.emptyList(),
                    rs.getString("shopName"),
                    rs.getString("address"),
                    rs.getString("email")
                );

                return new Book(
                    shop,
                    rs.getString("author"),
                    rs.getString("title"),
                    rs.getString("publisher"),
                    rs.getDate("publishDate"),
                    rs.getInt("pages"),
                    rs.getBoolean("isInReadingRoom")
                );
            },
            bookShopId
        );
    }
}
