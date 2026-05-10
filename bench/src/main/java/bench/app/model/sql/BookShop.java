package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class BookShop implements ConvertibleTo<bench.app.model.common.BookShop> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee manager;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OpeningHours openingHours;
    @OneToMany(fetch = FetchType.LAZY)
    private List<Book> bookOfferings;

    private String shopName;
    private String address;
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