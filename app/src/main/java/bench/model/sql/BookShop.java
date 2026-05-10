package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.List;

@Entity
public class BookShop implements ConvertibleTo<bench.model.common.BookShop> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Employee manager;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private OpeningHours openingHours;
    @ManyToOne(fetch = FetchType.LAZY)
    private List<Book> bookOfferings;

    private String shopName;
    private String address;
    private String email;

    @Override
    public bench.model.common.BookShop convertToModel() {
        return new bench.model.common.BookShop(
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