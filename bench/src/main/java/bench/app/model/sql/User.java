package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class User implements ConvertibleTo<bench.app.model.common.User> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BookShop mainBookShop;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserActivationStatus activationStatus;

    private String name;
    private String surname;
    private String phoneNumber;
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
