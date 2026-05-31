package bench.app.model.common;

public record BookShopCreateResult(
        String createdBookShopId,
        String shopName,
        String address,
        String email,
        String managerId,
        boolean restored,
        boolean existsAfterOperation,
        int insertedRows,
        int deletedRows
) {
}