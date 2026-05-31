package bench.app.benchmark;

import java.time.LocalDate;

public record BookReservationSnapshot(
        long bookId,
        long userId,
        LocalDate whenReserved
) {
}