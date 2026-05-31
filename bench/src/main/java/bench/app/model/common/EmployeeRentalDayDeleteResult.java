package bench.app.model.common;

import java.time.LocalDate;

public record EmployeeRentalDayDeleteResult(
        long employeeId,
        LocalDate rentalDate,
        int matchedRentals,
        int deletedRentals,
        int restoredRentals,
        boolean restored
) {
}