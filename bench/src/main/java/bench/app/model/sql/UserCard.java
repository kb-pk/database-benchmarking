package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class UserCard implements ConvertibleTo<bench.app.model.common.UserCard> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL)
    private User user;
    @OneToMany(cascade = CascadeType.ALL)
    private UserActivationStatus activationStatus;

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
