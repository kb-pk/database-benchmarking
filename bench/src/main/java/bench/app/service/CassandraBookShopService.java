package bench.app.service;

import bench.app.model.common.Book;
import bench.app.repository.cql.cassandra.CassandraBookShopRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CassandraBookShopService implements BookShopService<UUID> {
    private final CassandraBookShopRepository cassandraBookShopRepository;

    public CassandraBookShopService(CassandraBookShopRepository cassandraBookShopRepository) {
        this.cassandraBookShopRepository = cassandraBookShopRepository;
    }

    @Override
    public List<Book> getBooks(UUID bookShopId, boolean onlyAvailable) {
        throw new UnsupportedOperationException();
    }
}
