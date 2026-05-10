package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class UserActivationStatus implements ConvertibleTo<bench.model.common.UserActivationStatus> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String status;

    @Override
    public bench.model.common.UserActivationStatus convertToModel() {
        return new bench.model.common.UserActivationStatus(status);
    }
}
