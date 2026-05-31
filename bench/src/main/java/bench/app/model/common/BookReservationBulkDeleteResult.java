package bench.app.model.common;

public record BookReservationBulkDeleteResult(
        int monthsThreshold,
        int matchedReservations,
        int deletedReservations,
        int restoredReservations,
        boolean restored
) {
}