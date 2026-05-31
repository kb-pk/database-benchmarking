package bench.app.model.common;

public record BookRentalRanking(
    long bookId,
    String title,
    String author,
    long rentalCount,
    long rank
) {}
