package bench.app.model.common;

import java.time.LocalDate;

public record BookReservationDeleteResult(
        long deletedReservationId,
        long bookId,
        long userId,
        LocalDate whenReserved,
        boolean restored,
        boolean existsAfterOperation,
        int deletedRows,
        int insertedRows
) {
}