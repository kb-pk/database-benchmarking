package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "book")
public class Book implements ConvertibleTo<bench.app.model.common.Book> {
    @Id
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshopid")
    private BookShop bookShop;

    private String author;
    private String title;
    private String publisher;
    @Column(name = "publishdate")
    private Date publishDate;
    private int pages;
    @Column(name = "isinreadingroom")
    private boolean isInReadingRoom;

    @Override
    public bench.app.model.common.Book convertToModel() {
        return new bench.app.model.common.Book(
                bookShop == null ? null : bookShop.toShallowModel(),
                author, title,
                publisher, publishDate,
                pages, isInReadingRoom);
    }

    bench.app.model.common.Book toShallowModel() {
        return new bench.app.model.common.Book(
                bookShop == null ? null : bookShop.toShallowModel(),
                author,
                title,
                publisher,
                publishDate,
                pages,
                isInReadingRoom
        );
    }
}
