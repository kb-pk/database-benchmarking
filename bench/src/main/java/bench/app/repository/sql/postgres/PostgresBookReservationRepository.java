package bench.app.repository.sql.postgres;

import bench.app.model.sql.BookReservation;
import bench.app.model.sql.User;
import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserReservationCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Date;

@Repository
public interface PostgresBookReservationRepository extends JpaRepository<BookReservation, Integer> {
    List<BookReservation> findByBook_BookShop_Id(int shopId);

    @Query("SELECT new bench.app.model.common.UserReservationCount(u.id, u.name, u.surname, COUNT(r)) " +
           "FROM BookReservation r JOIN r.user u JOIN r.book b WHERE b.bookShop.id = :shopId " +
           "GROUP BY u.id, u.name, u.surname ORDER BY COUNT(r) DESC")
    List<UserReservationCount> findTopUsersByReservationCount(@Param("shopId") int shopId);

        @Query("SELECT new bench.app.model.common.UserReservationCount(u.id, u.name, u.surname, COUNT(r)) " +
            "FROM BookReservation r JOIN r.user u " +
            "GROUP BY u.id, u.name, u.surname ORDER BY COUNT(r) DESC")
        List<UserReservationCount> findTopUsersByReservationCountGlobal();

        @Query("SELECT new bench.app.model.common.EngagedUser(u.id, u.name, u.surname, u.phoneNumber, u.email) " +
            "FROM User u " +
            "WHERE EXISTS (" +
            "   SELECT 1 FROM BookReservation br " +
            "   WHERE br.user = u " +
            "     AND br.book.bookShop.id = :shopId " +
            "     AND br.whenReserved BETWEEN :fromDate AND :toDate" +
               ") " +
               "OR EXISTS (" +
            "   SELECT 1 FROM BookRental rt " +
            "   WHERE rt.user = u " +
            "     AND rt.bookShop.id = :shopId " +
            "     AND rt.startDate BETWEEN :fromDate AND :toDate" +
            ") " +
            "ORDER BY u.id")
        List<EngagedUser> findEngagedUsersByShopAndPeriod(
             @Param("shopId") int shopId,
             @Param("fromDate") Date fromDate,
             @Param("toDate") Date toDate
        );
}
