package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class UserCard implements ConvertibleTo<bench.model.common.UserCard> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @OneToOne(cascade = CascadeType.ALL)
    private User user;
    @OneToMany(cascade = CascadeType.ALL)
    private UserActivationStatus activationStatus;

    private String cardId;

    @Override
    public bench.model.common.UserCard convertToModel() {
        return new bench.model.common.UserCard(
                user.convertToModel(),
                activationStatus.convertToModel(),
                cardId
        );
    }
}
