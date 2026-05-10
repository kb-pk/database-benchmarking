package bench.model.common;

import java.util.Date;

public record BookReservation(
    Book book,
    User user,

    Date whenReserved
) {}
