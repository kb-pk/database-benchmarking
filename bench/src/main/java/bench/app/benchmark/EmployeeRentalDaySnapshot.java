package bench.app.benchmark;

import java.time.LocalDate;

public record EmployeeRentalDaySnapshot(
        long rentalId,
        long bookId,
        long userId,
        long employeeId,
        long bookShopId,
        long rentalMethodId,
        boolean isReturned,
        LocalDate startDate,
        LocalDate endDate
) {
}