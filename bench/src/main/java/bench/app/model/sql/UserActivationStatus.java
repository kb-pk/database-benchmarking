package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserActivationStatus implements ConvertibleTo<bench.app.model.common.UserActivationStatus> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String status;

    @Override
    public bench.app.model.common.UserActivationStatus convertToModel() {
        return new bench.app.model.common.UserActivationStatus(status);
    }
}
