package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class BookReservation implements ConvertibleTo<bench.app.model.common.BookReservation> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Book book;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;

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
