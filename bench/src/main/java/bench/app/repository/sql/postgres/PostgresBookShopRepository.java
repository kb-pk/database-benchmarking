package bench.app.repository.sql.postgres;

import bench.app.model.sql.BookShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostgresBookShopRepository extends JpaRepository<BookShop, Integer> {
}
