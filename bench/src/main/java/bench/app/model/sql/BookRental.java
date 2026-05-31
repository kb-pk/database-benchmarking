package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "bookrental")
public class BookRental implements ConvertibleTo<bench.app.model.common.BookRental> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookid")
    private Book book;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userid")
    private User user;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "employeeid")
    private Employee employee;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshopid")
    private BookShop bookShop;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "rentalmethodid")
    private RentalMethod rentalMethod;

    private boolean isReturned;
    private Date startDate;
    private Date endDate;

    @Override
    public bench.app.model.common.BookRental convertToModel() {
        return new bench.app.model.common.BookRental(
                book.convertToModel(),
                user.convertToModel(),
                employee.convertToModel(),
                bookShop.convertToModel(),
                rentalMethod.convertToModel(),
                isReturned, startDate, endDate
        );
    }
}
