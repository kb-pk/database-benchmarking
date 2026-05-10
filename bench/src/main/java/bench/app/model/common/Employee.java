package bench.app.model.common;

import java.util.Date;

public record Employee(
    BookShop primaryBookShop,

    String name,
    String surname,
    String phoneNumber,
    String email,
    Date birthDate,
    Date startedAt,
    String primaryBusinessRole
) {}
