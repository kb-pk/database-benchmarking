package bench.app.model.cassandra;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Table("employees_by_shop")
public class EmployeeByShop {
    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    @Column("primary_book_shop_id")
    private UUID primaryBookShopId;

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED)
    @Column("employee_id")
    private UUID employeeId;

    private String name;
    private String surname;
    @Column("phone_number")
    private String phoneNumber;
    private String email;
    @Column("birth_date")
    private LocalDate birthDate;
    @Column("started_at")
    private LocalDate startedAt;
    @Column("primary_business_role")
    private String primaryBusinessRole;
}