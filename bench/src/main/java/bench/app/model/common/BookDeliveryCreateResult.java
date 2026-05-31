package bench.app.model.common;

public record BookDeliveryCreateResult(
        long shopId,
        int batchSize,
        boolean restored,
        boolean existsAfterOperation,
        int insertedBooks,
        int insertedOfferings,
        int deletedBooks,
        int deletedOfferings
) {
}