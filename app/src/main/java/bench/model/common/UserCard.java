package bench.model.common;

public record UserCard(
    User user,
    UserActivationStatus activationStatus,

    String cardId
) {}
