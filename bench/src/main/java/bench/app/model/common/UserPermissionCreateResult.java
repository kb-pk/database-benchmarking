package bench.app.model.common;

public record UserPermissionCreateResult(
        long createdPermissionsId,
        String permission,
        String details,
        boolean restored,
        boolean existsAfterOperation,
        int insertedRows,
        int deletedRows
) {
}