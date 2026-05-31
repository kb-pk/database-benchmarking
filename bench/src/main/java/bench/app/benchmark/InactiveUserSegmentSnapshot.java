package bench.app.benchmark;

import java.time.LocalDate;
import java.util.List;

public record InactiveUserSegmentSnapshot(
        List<InactiveUserSnapshot> users
) {
    public record InactiveUserSnapshot(
            UserRow user,
            List<UserCardRow> cards,
            List<UserAccountRow> accounts,
            List<BookReservationRow> reservations,
            List<BookRentalRow> rentals
    ) {
    }

    public record UserRow(
            long id,
            String name,
            String surname,
            String phoneNumber,
            String email,
            Long mainBookShopId,
            long isActiveId
    ) {
    }

    public record UserCardRow(
            long id,
            String cardIdNumber,
            Long userId,
            long isActiveId
    ) {
    }

    public record UserAccountRow(
            long id,
            String login,
            String passwordHash,
            long userId,
            long permissionsId
    ) {
    }

    public record BookReservationRow(
            long id,
            long bookId,
            long userId,
            LocalDate whenReserved
    ) {
    }

    public record BookRentalRow(
            long id,
            long bookId,
            long userId,
            long employeeId,
            long bookShopId,
            boolean isReturned,
            LocalDate startDate,
            LocalDate endDate,
            long rentalMethodId
    ) {
    }
}