package bench.app.model.common;

public record BookRentalCloseOverdueResult(
        int daysThreshold,
        int matchedRentals,
        int closedRentals,
        int restoredRentals,
        boolean restored
) {
}