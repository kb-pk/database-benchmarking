package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "bench", name = "activationstatus")
public class UserActivationStatus implements ConvertibleTo<bench.app.model.common.UserActivationStatus> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "status")
    private String status;

    @Override
    public bench.app.model.common.UserActivationStatus convertToModel() {
        return new bench.app.model.common.UserActivationStatus(status);
    }
}
