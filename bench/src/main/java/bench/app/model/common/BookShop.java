package bench.app.model.common;

import java.util.List;

public record BookShop(
    Employee manager,
    OpeningHours openingHours,
    List<Book> bookOfferings,

    String shopName,
    String address,
    String email
) {}
