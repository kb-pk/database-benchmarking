package bench.app.model.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.Table;

import java.time.LocalTime;
import java.util.UUID;

@Table("bookshops")
public class BookShop {
    @PrimaryKey("shop_id")
    private UUID shopId;

    @Column("manager_id")
    private UUID managerId;

    @Column("shop_name")
    private String shopName;
    @Column("address")
    private String address;
    @Column("email")
    private String email;

    @Column("opens_at_monday")
    private LocalTime opensAtMonday;
    @Column("opens_at_tuesday")
    private LocalTime opensAtTuesday;
    @Column("opens_at_wednesday")
    private LocalTime opensAtWednesday;
    @Column("opens_at_thursday")
    private LocalTime opensAtThursday;
    @Column("opens_at_friday")
    private LocalTime opensAtFriday;
    @Column("opens_at_saturday")
    private LocalTime opensAtSaturday;
    @Column("opens_at_sunday")
    private LocalTime opensAtSunday;

    @Column("closes_at_monday")
    private LocalTime closesAtMonday;
    @Column("closes_at_tuesday")
    private LocalTime closesAtTuesday;
    @Column("closes_at_wednesday")
    private LocalTime closesAtWednesday;
    @Column("closes_at_thursday")
    private LocalTime closesAtThursday;
    @Column("closes_at_friday")
    private LocalTime closesAtFriday;
    @Column("closes_at_saturday")
    private LocalTime closesAtSaturday;
    @Column("closes_at_sunday")
    private LocalTime closesAtSunday;
}
