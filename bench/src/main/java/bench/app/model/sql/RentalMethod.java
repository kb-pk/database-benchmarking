package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class RentalMethod implements ConvertibleTo<bench.app.model.common.RentalMethod> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String method;

    @Override
    public bench.app.model.common.RentalMethod convertToModel() {
        return new  bench.app.model.common.RentalMethod(method);
    }
}
