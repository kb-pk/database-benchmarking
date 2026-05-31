package bench.app.model.common;

public record EngagedUser(
        long id,
        String name,
        String surname,
        String phoneNumber,
        String email
) {
}