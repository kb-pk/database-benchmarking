package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(schema = "bench", name = "bookrental")
public class BookRental implements ConvertibleTo<bench.app.model.common.BookRental> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookid", nullable = false)
    private Book book;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "employeeid", nullable = false)
    private Employee employee;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshopid", nullable = false)
    private BookShop bookShop;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "rentalmethodid", nullable = false)
    private RentalMethod rentalMethod;

    @Column(name = "isreturned", nullable = false)
    private short isReturned;
    @Temporal(TemporalType.DATE)
    @Column(name = "startdate", nullable = false)
    private Date startDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "enddate")
    private Date endDate;

    @Override
    public bench.app.model.common.BookRental convertToModel() {
        return new bench.app.model.common.BookRental(
                book.convertToModel(),
                user.convertToModel(),
                employee.convertToModel(),
                bookShop.convertToModel(),
                rentalMethod.convertToModel(),
                isReturned == 1, startDate, endDate
        );
    }
}
