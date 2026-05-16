package bench.app.model.cassandra;

import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.sql.Time;
import java.util.UUID;

@Table("bookshops")
public class BookShop {
    @PrimaryKey
    private UUID id;

    private Employee manager;

    private String shopName;
    private String address;
    private String email;

    private Time opensAtMonday;
    private Time opensAtTuesday;
    private Time opensAtWednesday;
    private Time opensAtThursday;
    private Time opensAtFriday;
    private Time opensAtSaturday;
    private Time opensAtSunday;

    private Time closesAtMonday;
    private Time closesAtTuesday;
    private Time closesAtWednesday;
    private Time closesAtThursday;
    private Time closesAtFriday;
    private Time closesAtSaturday;
    private Time closesAtSunday;
}
