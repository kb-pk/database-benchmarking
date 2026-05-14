package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.sql.Time;

@Entity
@Table(schema = "bench", name = "bookshopopeninghours")
public class OpeningHours implements ConvertibleTo<bench.app.model.common.OpeningHours> {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "opensatmonday")
    private Time opensAtMonday;
    @Column(name = "opensattuesday")
    private Time opensAtTuesday;
    @Column(name = "opensatwednesday")
    private Time opensAtWednesday;
    @Column(name = "opensatthursday")
    private Time opensAtThursday;
    @Column(name = "opensatfriday")
    private Time opensAtFriday;
    @Column(name = "opensatsaturday")
    private Time opensAtSaturday;
    @Column(name = "opensatsunday")
    private Time opensAtSunday;

    @Column(name = "closesatmonday")
    private Time closesAtMonday;
    @Column(name = "closesattuesday")
    private Time closesAtTuesday;
    @Column(name = "closesatwednesday")
    private Time closesAtWednesday;
    @Column(name = "closesatthursday")
    private Time closesAtThursday;
    @Column(name = "closesatfriday")
    private Time closesAtFriday;
    @Column(name = "closesatsaturday")
    private Time closesAtSaturday;
    @Column(name = "closesatsunday")
    private Time closesAtSunday;

    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "bookshopid", nullable = false)
    private BookShop bookShop;

    @Override
    public bench.app.model.common.OpeningHours convertToModel() {
        return new bench.app.model.common.OpeningHours(
                opensAtMonday, opensAtTuesday, opensAtWednesday,
                opensAtThursday, opensAtFriday,
                opensAtSaturday, opensAtSunday,
                closesAtMonday, closesAtTuesday, closesAtWednesday,
                closesAtThursday, closesAtFriday,
                closesAtSaturday, closesAtSunday
        );
    }
}
