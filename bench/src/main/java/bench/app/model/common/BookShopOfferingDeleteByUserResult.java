package bench.app.model.common;

public record BookShopOfferingDeleteByUserResult(
        long userId,
        int matchedOfferings,
        int deletedOfferings,
        int restoredOfferings,
        boolean restored
) {
}