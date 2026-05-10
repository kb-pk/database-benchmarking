package bench.app.model.common;

import java.util.Date;

public record BookRental(
    Book book,
    User user,
    Employee employee,
    BookShop bookShop,
    RentalMethod rentalMethod,

    boolean isReturned,
    Date startDate,
    Date endDate
) {}
