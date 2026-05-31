package bench.app.model.common;

public record UserActivationUpdateResult(
        long userId,
        long previousActivationStatusId,
        long requestedActivationStatusId,
        long finalActivationStatusId,
        boolean restored,
        int affectedUsers
) {
}