package bench.controller;

import bench.model.common.Book;
import bench.service.CassandraBookShopService;
import bench.service.SQLBookShopService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BookShopController {
    @Autowired
    private SQLBookShopService SQLBookShopService;
    @Autowired
    private CassandraBookShopService cassandraBookShopService;

    @GetMapping("/sql//bookshop/{bookShopId}/books")
    public List<Book> getAllBooksSQL(@PathVariable Long bookShopId, @RequestParam boolean onlyAvailable) {
        return this.SQLBookShopService.getBooks(bookShopId, onlyAvailable);
    }

    @GetMapping("/nosql/bookshop/{bookShopId}/books")
    public List<Book> getAllBooksNoSQL(@PathVariable Long bookShopId, @RequestParam boolean onlyAvailable) {
        return this.cassandraBookShopService.getBooks(bookShopId, onlyAvailable);
    }
}
