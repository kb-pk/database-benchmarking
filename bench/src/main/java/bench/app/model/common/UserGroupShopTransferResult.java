package bench.app.model.common;

public record UserGroupShopTransferResult(
        long sourceShopId,
        long targetShopId,
        int requestedMaxUsers,
        int movedUsers,
        int restoredUsers,
        boolean restored
) {
}