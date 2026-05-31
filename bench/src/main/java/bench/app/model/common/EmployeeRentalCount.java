package bench.app.model.common;

public record EmployeeRentalCount(
    long employeeId,
    String name,
    String surname,
    long rentalCount
) {}
