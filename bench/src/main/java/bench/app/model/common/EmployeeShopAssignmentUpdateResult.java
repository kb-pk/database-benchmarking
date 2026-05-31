package bench.app.model.common;

public record EmployeeShopAssignmentUpdateResult(
        long employeeId,
        long previousShopId,
        long requestedShopId,
        long finalShopId,
        boolean restored,
        int affectedEmployees
) {
}