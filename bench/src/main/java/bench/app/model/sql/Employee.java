package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
public class Employee implements ConvertibleTo<bench.app.model.common.Employee> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private BookShop primaryBookShop;

    private String name;
    private String surname;
    private String phoneNumber;
    private String email;
    private Date birthDate;
    private Date startedAt;
    private String primaryBusinessRole;

    @Override
    public bench.app.model.common.Employee convertToModel() {
        return new bench.app.model.common.Employee(
                primaryBookShop.convertToModel(),
                name, surname, phoneNumber, email,
                birthDate, startedAt,
                primaryBusinessRole
        );
    }
}
