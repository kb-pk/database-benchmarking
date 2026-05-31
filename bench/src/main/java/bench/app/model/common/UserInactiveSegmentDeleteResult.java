package bench.app.model.common;

public record UserInactiveSegmentDeleteResult(
        int monthsThreshold,
        int segmentSize,
        int matchedUsers,
        int deletedUsers,
        int deletedCards,
        int deletedAccounts,
        int deletedReservations,
        int deletedRentals,
        int restoredUsers,
        int restoredCards,
        int restoredAccounts,
        int restoredReservations,
        int restoredRentals,
        boolean restored
) {
}