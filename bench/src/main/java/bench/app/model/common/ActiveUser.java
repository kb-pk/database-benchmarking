package bench.app.model.common;

public record ActiveUser(
        long id,
        String name,
        String surname,
        String phoneNumber,
        String email,
        String status
) {
}
