package bench.app.model.common;

public record UserReservationCount(
    long userId,
    String name,
    String surname,
    long reservationCount
) {}
