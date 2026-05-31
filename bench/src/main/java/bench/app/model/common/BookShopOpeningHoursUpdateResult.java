package bench.app.model.common;

import java.time.LocalTime;

public record BookShopOpeningHoursUpdateResult(
        long shopId,
        LocalTime previousOpensAtMonday,
        LocalTime previousClosesAtMonday,
        LocalTime requestedOpensAtMonday,
        LocalTime requestedClosesAtMonday,
        LocalTime finalOpensAtMonday,
        LocalTime finalClosesAtMonday,
        boolean restored,
        int affectedRows
) {
}