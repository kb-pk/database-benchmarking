package bench.app.benchmark;

public record BookShopOfferingSnapshot(
        long offeringId,
        long bookId,
        long bookShopId
) {
}