package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class BookRental implements ConvertibleTo<bench.app.model.common.BookRental> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Book book;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee employee;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BookShop bookShop;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
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
