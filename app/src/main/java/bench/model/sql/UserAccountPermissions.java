package bench.model.sql;

import bench.model.common.ConvertibleTo;
import jakarta.persistence.*;

@Entity
public class UserAccountPermissions implements ConvertibleTo<bench.model.common.UserAccountPermissions> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    private String permission;
    private String details;

    @Override
    public bench.model.common.UserAccountPermissions convertToModel() {
        return new bench.model.common.UserAccountPermissions(
                permission,details
        );
    }
}
