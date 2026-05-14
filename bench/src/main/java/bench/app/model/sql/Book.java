package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(schema = "bench", name = "book")
public class Book implements ConvertibleTo<bench.app.model.common.Book> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshopid", nullable = false)
    private BookShop bookShop;

    @Column(name = "author", nullable = false)
    private String author;
    @Column(name = "title", nullable = false)
    private String title;
    @Column(name = "publisher")
    private String publisher;
    @Temporal(TemporalType.DATE)
    @Column(name = "publishdate")
    private Date publishDate;
    @Column(name = "pages")
    private int pages;
    @Column(name = "isinreadingroom", nullable = false)
    private short isInReadingRoom;

    @Override
    public bench.app.model.common.Book convertToModel() {
        return new bench.app.model.common.Book(
                bookShop.convertToModel(),
                author, title,
                publisher, publishDate,
                pages, isInReadingRoom == 1);
    }
}
