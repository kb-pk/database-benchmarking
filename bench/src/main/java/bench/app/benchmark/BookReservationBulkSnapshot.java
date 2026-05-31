package bench.app.benchmark;

import java.time.LocalDate;

public record BookReservationBulkSnapshot(
        long reservationId,
        long bookId,
        long userId,
        LocalDate whenReserved
) {
}