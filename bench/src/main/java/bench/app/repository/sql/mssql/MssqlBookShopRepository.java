package bench.app.repository.sql.mssql;

import bench.app.model.sql.BookShop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MssqlBookShopRepository extends JpaRepository<BookShop, Integer> {
}
