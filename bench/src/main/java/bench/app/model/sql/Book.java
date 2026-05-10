package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Book implements ConvertibleTo<bench.app.model.common.Book> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BookShop bookShop;

    private String author;
    private String title;
    private String publisher;
    private Date publishDate;
    private int pages;
    private boolean isInReadingRoom;

    @Override
    public bench.app.model.common.Book convertToModel() {
        return new bench.app.model.common.Book(
                bookShop.convertToModel(),
                author, title,
                publisher, publishDate,
                pages, isInReadingRoom);
    }
}
