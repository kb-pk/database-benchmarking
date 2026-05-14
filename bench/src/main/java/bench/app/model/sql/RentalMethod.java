package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "bench", name = "bookrentalmethod")
public class RentalMethod implements ConvertibleTo<bench.app.model.common.RentalMethod> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "method", nullable = false)
    private String method;

    @Override
    public bench.app.model.common.RentalMethod convertToModel() {
        return new  bench.app.model.common.RentalMethod(method);
    }
}
