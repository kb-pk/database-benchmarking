package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "bench", name = "useraccount")
public class UserAccount implements ConvertibleTo<bench.app.model.common.UserAccount> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userid", nullable = false)
    private User user;
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "permissionsid", nullable = false)
    private UserAccountPermissions userAccountPermissions;

    @Column(name = "login", nullable = false)
    private String login;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "passwordhash", nullable = false, columnDefinition = "CHAR(255)")
    private String passwordHash;

    @Override
    public bench.app.model.common.UserAccount convertToModel() {
        return new bench.app.model.common.UserAccount(
                user.convertToModel(),
                userAccountPermissions.convertToModel(),
                login, passwordHash
        );
    }
}
