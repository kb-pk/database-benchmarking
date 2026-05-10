package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class User implements ConvertibleTo<bench.model.common.User> {
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
    public bench.model.common.User convertToModel() {
        return new bench.model.common.User(
                mainBookShop.convertToModel(),
                activationStatus.convertToModel(),
                name, surname, phoneNumber, email
        );
    }
}
