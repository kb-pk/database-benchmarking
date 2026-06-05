package bench.app.model.common;

public record UserReservationRentalCount(
    long userId,
    String name,
    String surname,
    long reservationCount,
    long rentalCount,
    long totalCount
) {}