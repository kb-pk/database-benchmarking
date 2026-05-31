package bench.app.model.common;

import java.time.LocalDate;

public record BookReservationCreateResult(
        long createdReservationId,
        long bookId,
        long userId,
        LocalDate whenReserved,
        boolean restored,
        boolean existsAfterOperation,
        int insertedRows,
        int deletedRows
) {
}