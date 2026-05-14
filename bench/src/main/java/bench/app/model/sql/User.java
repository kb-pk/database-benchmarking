package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
@Table(schema = "bench", name = "bookshopuser")
public class User implements ConvertibleTo<bench.app.model.common.User> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "mainbookshopid")
    private BookShop mainBookShop;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "isactiveid", nullable = false)
    private UserActivationStatus activationStatus;

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "surname", nullable = false)
    private String surname;
    @Column(name = "phonenumber")
    private String phoneNumber;
    @Column(name = "email", nullable = false)
    private String email;

    @Override
    public bench.app.model.common.User convertToModel() {
        return new bench.app.model.common.User(
                mainBookShop.convertToModel(),
                activationStatus.convertToModel(),
                name, surname, phoneNumber, email
        );
    }
}
