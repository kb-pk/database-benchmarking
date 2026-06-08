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

import java.sql.Date;
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
        List<BookByShop> booksByShops = this.getBooksByShopRepo().findByShopId(bookShopId);
        return booksByShops.stream()
            .filter(book -> !onlyAvailable || !book.isInReadingRoom())
            .map(x -> new Book(
                null,
                x.getAuthor(),
                x.getTitle(),
                x.getPublisher(),
                x.getPublishDate() != null ? Date.valueOf(x.getPublishDate()) : null,
                x.getPages(),
                x.isInReadingRoom()
            ))
            .toList();
    }

        protected bench.app.model.common.BookShop toShallowBookShopModel(bench.app.model.common.BookShop shop) {
                return new bench.app.model.common.BookShop(
                                null,
                                shop.getOpeningHours(),
                                List.of(),
                                shop.getShopName(),
                                shop.getAddress(),
                                shop.getEmail()
                );
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
        bench.app.model.common.BookShop shallowBookShop = this.toShallowBookShopModel(s);

        // lazily instantiate shop for manager
        if (shop.getManagerId() != null) {
            try {
                Employee manager = this.getDeferredEmployeeService().getDeferredEmployee(shop.getManagerId())
                        .instantiateWith(shallowBookShop);
                s.setManager(manager);
            } catch (IllegalArgumentException ignored) {
                s.setManager(null);
            }
        }

        // fill in books
        List<BookByShop> booksByShops = this.getBooksByShopRepo().findByShopId(shop.getId());
        bookOfferings.addAll(booksByShops.stream()
                .map(x -> new Book(
                        shallowBookShop,
                        x.getAuthor(), x.getTitle(), x.getPublisher(),
                        x.getPublishDate() != null ? Date.valueOf(x.getPublishDate()) : null,
                        x.getPages(), x.isInReadingRoom())
                ).toList()
        );

        return s;
    }
}
