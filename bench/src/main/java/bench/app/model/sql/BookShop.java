package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(schema = "bench", name = "bookshop")
public class BookShop implements ConvertibleTo<bench.app.model.common.BookShop> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "managerid", nullable = false)
    private Employee manager;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "openinghoursid")
    private OpeningHours openingHours;
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(
            schema = "bench",
            name = "bookshopoffering",
            joinColumns = @JoinColumn(name = "bookshopid"),
            inverseJoinColumns = @JoinColumn(name = "bookid")
    )
    private List<Book> bookOfferings;

    @Column(name = "shopname", nullable = false)
    private String shopName;
    @Column(name = "address", nullable = false)
    private String address;
    @Column(name = "email", nullable = false)
    private String email;

    @Override
    public bench.app.model.common.BookShop convertToModel() {
        return new bench.app.model.common.BookShop(
                manager.convertToModel(),
                openingHours.convertToModel(),
                bookOfferings
                        .stream()
                        .map(Book::convertToModel)
                        .toList(),
                shopName, address, email
        );
    }
}