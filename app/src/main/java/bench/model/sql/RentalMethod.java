package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class RentalMethod implements ConvertibleTo<bench.model.common.RentalMethod> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String method;

    @Override
    public bench.model.common.RentalMethod convertToModel() {
        return new  bench.model.common.RentalMethod(method);
    }
}
