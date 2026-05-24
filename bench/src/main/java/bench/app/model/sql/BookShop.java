package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "bookshop")
public class BookShop implements ConvertibleTo<bench.app.model.common.BookShop> {
    @Id
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "managerid")
    private Employee manager;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "openinghoursid")
    private OpeningHours openingHours;
    @OneToMany(mappedBy = "bookShop", fetch = FetchType.LAZY)
    private List<Book> bookOfferings;

    @Column(name = "shopname")
    private String shopName;
    private String address;
    private String email;

    @Override
    public bench.app.model.common.BookShop convertToModel() {
        return new bench.app.model.common.BookShop(
            manager == null ? null : manager.toShallowModel(),
                openingHours.convertToModel(),
                bookOfferings
                        .stream()
                        .map(Book::convertToModel)
                        .toList(),
                shopName, address, email
        );
    }

        bench.app.model.common.BookShop toShallowModel() {
        return new bench.app.model.common.BookShop(
            null,
            null,
            List.of(),
            shopName,
            address,
            email
        );
        }
}