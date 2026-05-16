package bench.app.controller;

import bench.app.model.common.Book;
import bench.app.service.CassandraBookShopService;
import bench.app.service.SQLBookShopService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class BookShopController {
    private final SQLBookShopService sqlBookShopService;
    private final CassandraBookShopService cassandraBookShopService;

    // Spring will automatically inject the services through this constructor
    public BookShopController(SQLBookShopService sqlBookShopService, CassandraBookShopService cassandraBookShopService) {
        this.sqlBookShopService = sqlBookShopService;
        this.cassandraBookShopService = cassandraBookShopService;
    }
    @GetMapping("/sql//bookshop/{bookShopId}/books")
    public List<Book> getAllBooksSQL(@PathVariable Long bookShopId, @RequestParam boolean onlyAvailable) {
        return this.sqlBookShopService.getBooks(bookShopId, onlyAvailable);
    }

    @GetMapping("/nosql/bookshop/{bookShopId}/books")
    public List<Book> getAllBooksNoSQL(@PathVariable UUID bookShopId, @RequestParam boolean onlyAvailable) {
        return this.cassandraBookShopService.getBooks(bookShopId, onlyAvailable);
    }
}
