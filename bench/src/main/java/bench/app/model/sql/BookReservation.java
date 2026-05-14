package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(schema = "bench", name = "bookreservation")
public class BookReservation implements ConvertibleTo<bench.app.model.common.BookReservation> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookid", nullable = false)
    private Book book;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;

    @Temporal(TemporalType.DATE)
    @Column(name = "whenreserved", nullable = false)
    private Date whenReserved;

    @Override
    public bench.app.model.common.BookReservation convertToModel() {
        return new bench.app.model.common.BookReservation(
                book.convertToModel(),
                user.convertToModel(),
                whenReserved
        );
    }
}
