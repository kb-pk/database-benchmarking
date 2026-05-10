package bench.app.model.common;

import java.sql.Time;

public record OpeningHours(
    Time opensAtMonday,
    Time opensAtTuesday,
    Time opensAtWednesday,
    Time opensAtThursday,
    Time opensAtFriday,
    Time opensAtSaturday,
    Time opensAtSunday,

    Time closesAtMonday,
    Time closesAtTuesday,
    Time closesAtWednesday,
    Time closesAtThursday,
    Time closesAtFriday,
    Time closesAtSaturday,
    Time closesAtSunday
) {}
