package bench.app.model.common;

import java.time.LocalDate;

public record BookRentalDeleteResult(
        long deletedRentalId,
        long bookId,
        long userId,
        long employeeId,
        long bookShopId,
        long rentalMethodId,
        boolean isReturned,
        LocalDate startDate,
        LocalDate endDate,
        boolean restored,
        boolean existsAfterOperation,
        int deletedRows,
        int insertedRows
) {
}