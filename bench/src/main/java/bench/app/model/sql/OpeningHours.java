package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.*;

import java.sql.Time;

@Entity
@Table(name = "bookshopopeninghours")
public class OpeningHours implements ConvertibleTo<bench.app.model.common.OpeningHours> {
    @Id
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
