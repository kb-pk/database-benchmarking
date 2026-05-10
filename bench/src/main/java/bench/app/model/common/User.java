package bench.app.model.common;

public record User(
    BookShop mainBookShop,
    UserActivationStatus activationStatus,

    String name,
    String surname,
    String phoneNumber,
    String email
) {}
