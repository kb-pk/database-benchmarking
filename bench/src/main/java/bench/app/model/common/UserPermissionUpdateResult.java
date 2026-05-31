package bench.app.model.common;

public record UserPermissionUpdateResult(
        long userId,
        long previousPermissionsId,
        long requestedPermissionsId,
        long finalPermissionsId,
        boolean restored,
        int affectedAccounts
) {
}