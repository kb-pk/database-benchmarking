package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class BookRental implements ConvertibleTo<bench.model.common.BookRental> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Book book;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee employee;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BookShop bookShop;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private RentalMethod rentalMethod;

    private boolean isReturned;
    private Date startDate;
    private Date endDate;

    @Override
    public bench.model.common.BookRental convertToModel() {
        return new bench.model.common.BookRental(
                book.convertToModel(),
                user.convertToModel(),
                employee.convertToModel(),
                bookShop.convertToModel(),
                rentalMethod.convertToModel(),
                isReturned, startDate, endDate
        );
    }
}
