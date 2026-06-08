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
public class PostgresBookRentalService {
    private static final String SELECT_RENTALS_BY_SHOP = """
        SELECT br.id,
           br.isreturned,
           br.startdate,
           br.enddate,
           b.author,
           b.title,
           b.publisher,
           b.publishdate,
           b.pages,
           b.isinreadingroom,
           bs.shopname,
           bs.address,
           bs.email,
           u.name AS user_name,
           u.surname AS user_surname,
           u.phonenumber AS user_phone,
           u.email AS user_email,
           ast.status AS user_status,
           e.name AS employee_name,
           e.surname AS employee_surname,
           e.phonenumber AS employee_phone,
           e.email AS employee_email,
           e.birthdate,
           e.startedat,
           e.primarybusinessrole,
           rm.method AS rental_method
        FROM bench.bookrental br
        JOIN bench.book b ON b.id = br.bookid
        JOIN bench.bookshop bs ON bs.id = br.bookshopid
        JOIN bench.bookshopuser u ON u.id = br.userid
        JOIN bench.activationstatus ast ON ast.id = u.isactiveid
        JOIN bench.employee e ON e.id = br.employeeid
        JOIN bench.bookrentalmethod rm ON rm.id = br.rentalmethodid
        WHERE br.bookshopid = ?
        ORDER BY br.id
        """;

    private final JdbcTemplate jdbcTemplate;

    public PostgresBookRentalService(@Qualifier("postgresDataSource") DataSource dataSource) {
    this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(readOnly = true, transactionManager = "postgresTransactionManager")
    public List<BookRental> getBookRentalsByShopId(long shopId) {
    return jdbcTemplate.query(
        SELECT_RENTALS_BY_SHOP,
        (rs, rowNum) -> {
            BookShop shop = new BookShop(
                null,
                null,
                Collections.emptyList(),
                rs.getString("shopname"),
                rs.getString("address"),
                rs.getString("email")
            );

            Book book = new Book(
                shop,
                rs.getString("author"),
                rs.getString("title"),
                rs.getString("publisher"),
                rs.getDate("publishdate"),
                rs.getInt("pages"),
                rs.getBoolean("isinreadingroom")
            );

            User user = new User(
                null,
                new UserActivationStatus(rs.getString("user_status")),
                rs.getString("user_name"),
                rs.getString("user_surname"),
                rs.getString("user_phone"),
                rs.getString("user_email")
            );

            Employee employee = new Employee(
                null,
                rs.getString("employee_name"),
                rs.getString("employee_surname"),
                rs.getString("employee_phone"),
                rs.getString("employee_email"),
                rs.getDate("birthdate"),
                rs.getDate("startedat"),
                rs.getString("primarybusinessrole")
            );

            return new BookRental(
                book,
                user,
                employee,
                shop,
                new RentalMethod(rs.getString("rental_method")),
                rs.getBoolean("isreturned"),
                rs.getDate("startdate"),
                rs.getDate("enddate")
            );
        },
        shopId
    );
    }
}
