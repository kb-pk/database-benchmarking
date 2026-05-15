package bench.app;

import bench.app.service.userpermission.UserPermissionCrudOperations;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
public class CreateBenchmarkRunner implements ApplicationRunner {
    private final UserPermissionCrudOperations userPermissionCrudOperations;

    public CreateBenchmarkRunner(UserPermissionCrudOperations userPermissionCrudOperations) {
        this.userPermissionCrudOperations = userPermissionCrudOperations;
    }

    @Override
    public void run(ApplicationArguments args) {
        String loadSqlArg = getFirstOption(args, "bench.load-sql");

        if (isBlank(loadSqlArg)) {
            return;
        }

        System.out.println("[bench] Uruchamiam benchmark CREATE po zaladowaniu danych.");
        boolean hasErrors = false;

        try {
            runStep("CREATE 1", () -> userPermissionCrudOperations.create(
                null,
                "WYPOZYCZENIE_BENCHMARK",
                "Rekord testowy dla operacji CREATE 1"
            ));

            runStep("CREATE 2", () -> userPermissionCrudOperations.createRentalMethod(
                null,
                "RENTAL_BENCHMARK"
            ));

            runStep("CREATE 3", () -> userPermissionCrudOperations.createBookShop(
                null,
                "Sklep Testowy",
                "ul. Benchmarkowa 123",
                "sklep@benchmark.test",
                null
            ));

            runStep("CREATE 4", () -> userPermissionCrudOperations.createUserRegistration(
                null,
                "Jan",
                "Kowalski",
                "+48123123123",
                "jan.kowalski@example.com",
                "jkowalski_bench",
                "sha256$abc$hash",
                null,
                null,
                null,
                null
            ));

            runStep("CREATE 5", () -> userPermissionCrudOperations.createBookWithOffering(
                null,
                "Projekt Testowy",
                "Autor Testowy",
                null,
                null
            ));

            runStep("CREATE 6", () -> userPermissionCrudOperations.createBookReservation(
                null,
                null,
                null
            ));

            runStep("CREATE 7", () -> userPermissionCrudOperations.createRentalWithFullContext(
                null,
                null,
                null,
                null,
                null,
                null
            ));

            runStep("CREATE 8", () -> userPermissionCrudOperations.createConditionalRental(
                null
            ));

            runStep("CREATE 9", () -> userPermissionCrudOperations.createBatchSupplyEvent(
                null,
                5,
                null
            ));

            System.out.println("[bench] Zakonczono benchmark CREATE i zapis CSV.");
        } catch (Exception ex) {
            System.err.println("[bench] Blad w trakcie benchmark CREATE: " + ex.getMessage());
            ex.printStackTrace();
            hasErrors = true;
        }

        // Wymuszenie zakonczenia aplikacji
        if (hasErrors) {
            System.err.println("[bench] Zakonczanie aplikacji z bledem (exit code 1)");
            System.exit(1);
        } else {
            System.out.println("[bench] Zakonczanie aplikacji pomyslnie (exit code 0)");
            System.exit(0);
        }
    }

        private void runStep(String label, Runnable action) {
        try {
            action.run();
            System.out.println("[bench] " + label + " zakonczony.");
        } catch (Exception ex) {
            System.err.println("[bench] " + label + " nieudany: " + ex.getMessage());
        }
        }

    private String getFirstOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            return null;
        }
        var values = args.getOptionValues(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}