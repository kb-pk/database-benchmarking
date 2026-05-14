package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(schema = "bench", name = "employee")
public class Employee implements ConvertibleTo<bench.app.model.common.Employee> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "primarybookshopid")
    private BookShop primaryBookShop;

    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "surname", nullable = false)
    private String surname;
    @Column(name = "phonenumber", nullable = false)
    private String phoneNumber;
    @Column(name = "email", nullable = false)
    private String email;
    @Temporal(TemporalType.DATE)
    @Column(name = "birthdate")
    private Date birthDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "startedat")
    private Date startedAt;
    @Column(name = "primarybusinessrole")
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
