package bench.app.repository.sql.postgres;

import bench.app.model.sql.BookRental;
import bench.app.model.sql.Employee;
import bench.app.model.common.EmployeeRentalCount;
import bench.app.model.common.BookRentalRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostgresBookRentalRepository extends JpaRepository<BookRental, Integer> {
    List<BookRental> findByBook_BookShop_Id(int shopId);

    @Query("SELECT new bench.app.model.common.EmployeeRentalCount(e.id, e.name, e.surname, COUNT(r)) " +
        "FROM BookRental r JOIN r.employee e JOIN r.bookShop s WHERE s.id = :shopId " +
        "GROUP BY e.id, e.name, e.surname ORDER BY COUNT(r) DESC")
    List<EmployeeRentalCount> findEmployeeRentalCountsByShop(@Param("shopId") int shopId);

    @Query("SELECT new bench.app.model.common.EmployeeRentalCount(e.id, e.name, e.surname, COUNT(r)) " +
        "FROM BookRental r JOIN r.employee e " +
        "GROUP BY e.id, e.name, e.surname ORDER BY COUNT(r) DESC")
    List<EmployeeRentalCount> findEmployeeRentalCountsGlobal();

    @Query(value = "WITH ranked_books AS ( " +
        "  SELECT b.id as book_id, b.title, b.author, COUNT(*) as rental_count, " +
        "         RANK() OVER (ORDER BY COUNT(*) DESC) as rank " +
        "  FROM bookrental r " +
        "  JOIN book b ON r.bookid = b.id " +
        "  WHERE b.bookshopid = :shopId " +
        "  GROUP BY b.id, b.title, b.author " +
        ") " +
        "SELECT book_id, title, author, rental_count, rank FROM ranked_books ORDER BY rank, book_id", 
        nativeQuery = true)
    List<Object[]> findBookRentalRankingByShop(@Param("shopId") int shopId);
}
