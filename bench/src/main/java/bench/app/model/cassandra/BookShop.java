package bench.app.model.cassandra;

import lombok.Data;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.sql.Time;
import java.util.UUID;

@Data
@Table("bookshops")
public class BookShop {
    @PrimaryKey
    private UUID id;

    @Column("manager_id")
    private UUID managerId;

    @Column("shop_name")
    private String shopName;
    private String address;
    private String email;

    @Column("opens_at_monday")
    private Time opensAtMonday;
    @Column("opens_at_tuesday")
    private Time opensAtTuesday;
    @Column("opens_at_wednesday")
    private Time opensAtWednesday;
    @Column("opens_at_thursday")
    private Time opensAtThursday;
    @Column("opens_at_friday")
    private Time opensAtFriday;
    @Column("opens_at_saturday")
    private Time opensAtSaturday;
    @Column("opens_at_sunday")
    private Time opensAtSunday;

    @Column("closes_at_monday")
    private Time closesAtMonday;
    @Column("closes_at_tuesday")
    private Time closesAtTuesday;
    @Column("closes_at_wednesday")
    private Time closesAtWednesday;
    @Column("closes_at_thursday")
    private Time closesAtThursday;
    @Column("closes_at_friday")
    private Time closesAtFriday;
    @Column("closes_at_saturday")
    private Time closesAtSaturday;
    @Column("closes_at_sunday")
    private Time closesAtSunday;
}
