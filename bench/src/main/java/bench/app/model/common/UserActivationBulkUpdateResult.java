package bench.app.model.common;

public record UserActivationBulkUpdateResult(
        long requestedActivationStatusId,
        int matchedUsers,
        int updatedUsers,
        int restoredUsers,
        boolean restored
) {
}
