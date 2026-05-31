package bench.app.model.common;

public record UserRegistrationCreateResult(
        long createdUserId,
        long createdUserCardId,
        long createdUserAccountId,
        String login,
        String email,
        boolean restored,
        boolean existsAfterOperation,
        int insertedRows,
        int deletedRows
) {
}