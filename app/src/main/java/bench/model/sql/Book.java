package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.util.Date;

@Entity
public class Book implements ConvertibleTo<bench.model.common.Book> {
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
    public bench.model.common.Book convertToModel() {
        return new bench.model.common.Book(
                bookShop.convertToModel(),
                author, title,
                publisher, publishDate,
                pages, isInReadingRoom);
    }
}
