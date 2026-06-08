package bench.app.service.sql;

import bench.app.model.common.BookRental;
import bench.app.model.common.Book;
import bench.app.model.common.BookShop;
import bench.app.model.common.Employee;
import bench.app.model.common.RentalMethod;
import bench.app.model.common.User;
import bench.app.model.common.UserActivationStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.List;

@Service
public class MssqlBookRentalService {
    private static final String SELECT_RENTALS_BY_SHOP = """
        SELECT br.id,
           br.isReturned,
           br.startDate,
           br.endDate,
           b.author,
           b.title,
           b.publisher,
           b.publishDate,
           b.pages,
           b.isInReadingRoom,
           bs.shopName,
           bs.address,
           bs.email,
           u.name AS userName,
           u.surname AS userSurname,
           u.phoneNumber AS userPhone,
           u.email AS userEmail,
           ast.status AS userStatus,
           e.name AS employeeName,
           e.surname AS employeeSurname,
           e.phoneNumber AS employeePhone,
           e.email AS employeeEmail,
           e.birthDate,
           e.startedAt,
           e.primaryBusinessRole,
           rm.method AS rentalMethod
        FROM bench.BookRental br
        JOIN bench.Book b ON b.id = br.bookId
        JOIN bench.BookShop bs ON bs.id = br.bookShopId
        JOIN bench.BookShopUser u ON u.id = br.userId
        JOIN bench.ActivationStatus ast ON ast.id = u.isActiveId
        JOIN bench.Employee e ON e.id = br.employeeId
        JOIN bench.BookRentalMethod rm ON rm.id = br.rentalMethodId
        WHERE br.bookShopId = ?
        ORDER BY br.id
        """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookRentalService(@Qualifier("mssqlDataSource") DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(readOnly = true, transactionManager = "mssqlTransactionManager")
    public List<BookRental> getBookRentalsByShopId(long shopId) {
    return jdbcTemplate.query(
        SELECT_RENTALS_BY_SHOP,
        (rs, rowNum) -> {
            BookShop shop = new BookShop(
                null,
                null,
                Collections.emptyList(),
                rs.getString("shopName"),
                rs.getString("address"),
                rs.getString("email")
            );

            Book book = new Book(
                shop,
                rs.getString("author"),
                rs.getString("title"),
                rs.getString("publisher"),
                rs.getDate("publishDate"),
                rs.getInt("pages"),
                rs.getBoolean("isInReadingRoom")
            );

            User user = new User(
                null,
                new UserActivationStatus(rs.getString("userStatus")),
                rs.getString("userName"),
                rs.getString("userSurname"),
                rs.getString("userPhone"),
                rs.getString("userEmail")
            );

            Employee employee = new Employee(
                null,
                rs.getString("employeeName"),
                rs.getString("employeeSurname"),
                rs.getString("employeePhone"),
                rs.getString("employeeEmail"),
                rs.getDate("birthDate"),
                rs.getDate("startedAt"),
                rs.getString("primaryBusinessRole")
            );

            return new BookRental(
                book,
                user,
                employee,
                shop,
                new RentalMethod(rs.getString("rentalMethod")),
                rs.getBoolean("isReturned"),
                rs.getDate("startDate"),
                rs.getDate("endDate")
            );
        },
        shopId
    );
    }
}
