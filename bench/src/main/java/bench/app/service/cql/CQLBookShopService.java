package bench.app.service.cql;

import bench.app.model.cassandra.BookShop;
import bench.app.model.cassandra.BookByShop;
import bench.app.model.common.Book;
import bench.app.model.common.Employee;
import bench.app.model.common.OpeningHours;
import bench.app.repository.cql.BooksByShopRepository;
import bench.app.repository.cql.EmployeesByShopRepository;
import bench.app.service.BookShopService;
import org.springframework.data.cassandra.repository.CassandraRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class CQLBookShopService<
        BookShopRepo extends CassandraRepository<BookShop, UUID>,
        BooksByShopRepo extends BooksByShopRepository,
        EmployeeByShopRepo extends EmployeesByShopRepository,
        DeferredEmployeeService extends CQLDeferredEmployeeService<EmployeeByShopRepo>
        >
        implements BookShopService<UUID> {
    protected abstract BookShopRepo getBookShopRepo();
    protected abstract BooksByShopRepo getBooksByShopRepo();
    protected abstract DeferredEmployeeService getDeferredEmployeeService();

    @Override
    public List<bench.app.model.common.BookShop> getBookShops() {
        return this.getBookShopRepo().findAll().stream()
                .map(this::toBookShopModel)
                .toList();
    }

    public bench.app.model.common.BookShop getBookShop(UUID id) {
        BookShop shop = this.getBookShopRepo().findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bookshop not found"));

        return this.toBookShopModel(shop);
    }

    @Override
    public List<bench.app.model.common.Book> getBooks(UUID bookShopId, boolean onlyAvailable) {
        return this.getBookShop(bookShopId).getBookOfferings();
    }

    private bench.app.model.common.BookShop toBookShopModel(BookShop shop) {
        List<bench.app.model.common.Book> bookOfferings = new ArrayList<>();
        bench.app.model.common.BookShop s = new bench.app.model.common.BookShop(
                null, new OpeningHours(
                shop.getOpensAtMonday(), shop.getOpensAtTuesday(), shop.getOpensAtWednesday(),
                shop.getOpensAtThursday(), shop.getOpensAtFriday(), shop.getOpensAtSaturday(), shop.getOpensAtSunday(),
                shop.getClosesAtMonday(), shop.getClosesAtTuesday(), shop.getClosesAtWednesday(),
                shop.getClosesAtThursday(), shop.getClosesAtFriday(), shop.getClosesAtSaturday(), shop.getClosesAtSunday()
        ),
                bookOfferings,
                shop.getShopName(), shop.getAddress(), shop.getEmail()
        );

        // lazily instantiate shop for manager
        Employee manager = this.getDeferredEmployeeService().getDeferredEmployee(shop.getManagerId())
                .instantiateWith(s);
        s.setManager(manager);

        // fill in books
        List<BookByShop> booksByShops = this.getBooksByShopRepo().findByShopId(shop.getId());
        bookOfferings.addAll(booksByShops.stream()
                .map(x -> new Book(
                        s,
                        x.getAuthor(), x.getTitle(), x.getPublisher(), x.getPublishDate(), x.getPages(), x.isInReadingRoom())
                ).toList()
        );

        return s;
    }
}
