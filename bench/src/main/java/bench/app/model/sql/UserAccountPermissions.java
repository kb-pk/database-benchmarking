package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(schema = "bench", name = "useraccountpermissions")
public class UserAccountPermissions implements ConvertibleTo<bench.app.model.common.UserAccountPermissions> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "permission", nullable = false)
    private String permission;
    @Column(name = "details")
    private String details;

    @Override
    public bench.app.model.common.UserAccountPermissions convertToModel() {
        return new bench.app.model.common.UserAccountPermissions(
                permission,details
        );
    }
}
