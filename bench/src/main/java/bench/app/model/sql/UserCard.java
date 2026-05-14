package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "bench", name = "usercard")
public class UserCard implements ConvertibleTo<bench.app.model.common.UserCard> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "userid")
    private User user;
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "isactiveid", nullable = false)
    private UserActivationStatus activationStatus;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cardidnumber", nullable = false, length = 30, columnDefinition = "CHAR(30)")
    private String cardId;

    @Override
    public bench.app.model.common.UserCard convertToModel() {
        return new bench.app.model.common.UserCard(
                user.convertToModel(),
                activationStatus.convertToModel(),
                cardId
        );
    }
}
