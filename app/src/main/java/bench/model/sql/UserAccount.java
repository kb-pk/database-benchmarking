package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class UserAccount implements ConvertibleTo<bench.model.common.UserAccount> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private User user;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserAccountPermissions userAccountPermissions;

    private String login;
    private String passwordHash;

    @Override
    public bench.model.common.UserAccount convertToModel() {
        return new bench.model.common.UserAccount(
                user.convertToModel(),
                userAccountPermissions.convertToModel(),
                login, passwordHash
        );
    }
}
