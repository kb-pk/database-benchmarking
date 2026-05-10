package bench.app.model.common;

public record UserAccount(
    User user,
    UserAccountPermissions userAccountPermissions,

    String login,
    String passwordHash
) {}
