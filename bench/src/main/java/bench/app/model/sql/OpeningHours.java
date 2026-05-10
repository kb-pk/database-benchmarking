package bench.app.model.sql;

import bench.app.model.common.ConvertibleTo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.sql.Time;

@Entity
public class OpeningHours implements ConvertibleTo<bench.app.model.common.OpeningHours> {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

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
