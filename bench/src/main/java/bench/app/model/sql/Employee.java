package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "employee")
public class Employee implements ConvertibleTo<bench.app.model.common.Employee> {
    @Id
    private int id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primarybookshopid")
    private BookShop primaryBookShop;

    private String name;
    private String surname;
    @Column(name = "phonenumber")
    private String phoneNumber;
    private String email;
    @Column(name = "birthdate")
    private Date birthDate;
    @Column(name = "startedat")
    private Date startedAt;
    @Column(name = "primarybusinessrole")
    private String primaryBusinessRole;

    @Override
    public bench.app.model.common.Employee convertToModel() {
        return new bench.app.model.common.Employee(
                primaryBookShop == null ? null : primaryBookShop.toShallowModel(),
                name, surname, phoneNumber, email,
                birthDate, startedAt,
                primaryBusinessRole
        );
    }

    bench.app.model.common.Employee toShallowModel() {
        return new bench.app.model.common.Employee(
                null,
                name,
                surname,
                phoneNumber,
                email,
                birthDate,
                startedAt,
                primaryBusinessRole
        );
    }
}
