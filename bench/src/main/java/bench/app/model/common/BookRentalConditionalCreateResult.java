package bench.app.model.common;

import java.time.LocalDate;

public record BookRentalConditionalCreateResult(
        long createdRentalId,
        long shopId,
        long bookId,
        long userId,
        long employeeId,
        long rentalMethodId,
        LocalDate startDate,
        boolean restored,
        boolean existsAfterOperation,
        int insertedRows,
        int deletedRows
) {
}