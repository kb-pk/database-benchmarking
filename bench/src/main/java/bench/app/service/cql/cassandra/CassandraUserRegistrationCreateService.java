package bench.app.service.cql.cassandra;

import bench.app.model.common.UserRegistrationCreateResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class CassandraUserRegistrationCreateService {
    private static final String INSERT_USER = """
            INSERT INTO users (
                user_id, name, surname, phone_number, email,
                main_book_shop_id, card_id_number, status, login, permissions
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_CREDENTIALS = """
            INSERT INTO user_credentials_by_login (login, user_id, password_hash, status)
            VALUES (?, ?, ?, ?)
            """;

    private static final String DELETE_CREDENTIALS = "DELETE FROM user_credentials_by_login WHERE login = ?";
    private static final String DELETE_USER = "DELETE FROM users WHERE user_id = ?";

    private final CqlSession cassandraSession;

    public CassandraUserRegistrationCreateService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public UserRegistrationCreateResult createUserRegistration(
            String name,
            String surname,
            String phoneNumber,
            String email,
            String login,
            String passwordHash,
            boolean restoreAfterCreate
    ) {
        validateInput(name, surname, email, login, passwordHash);

        UUID mainShopId = findAnyShopId();
        if (mainShopId == null) {
            throw new IllegalArgumentException("Brak sklepu referencyjnego w tabeli bookshops");
        }

        UUID userId = UUID.randomUUID();
        String cardIdNumber = "CARD-" + userId.toString().replace("-", "");
        String status = "ACTIVE";

        int insertedRows = 0;
        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_USER)
                        .addPositionalValues(
                                userId,
                                name,
                                surname,
                                blankToNull(phoneNumber),
                                email,
                                mainShopId,
                                cardIdNumber,
                                status,
                                login,
                                Set.of("BASIC_USER")
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        insertedRows += cassandraSession.execute(
                SimpleStatement.builder(INSERT_CREDENTIALS)
                        .addPositionalValues(login, userId, passwordHash, status)
                        .build()
        ).wasApplied() ? 1 : 0;

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_CREDENTIALS)
                            .addPositionalValue(login)
                            .build()
            ).wasApplied() ? 1 : 0;
            deletedRows += cassandraSession.execute(
                    SimpleStatement.builder(DELETE_USER)
                            .addPositionalValue(userId)
                            .build()
            ).wasApplied() ? 1 : 0;
            existsAfterOperation = false;
        }

        long mappedUserId = uuidToPositiveLong(userId);
        return new UserRegistrationCreateResult(
                mappedUserId,
                mappedUserId,
                mappedUserId,
                login,
                email,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }

    private UUID findAnyShopId() {
        Row row = cassandraSession.execute("SELECT shop_id FROM bookshops LIMIT 1").one();
        return row == null ? null : row.getUuid("shop_id");
    }

    private static void validateInput(String name, String surname, String email, String login, String passwordHash) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name nie może być puste");
        }
        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException("surname nie może być puste");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email nie może być pusty");
        }
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login nie może być pusty");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash nie może być pusty");
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static long uuidToPositiveLong(UUID value) {
        long raw = value.getMostSignificantBits() ^ value.getLeastSignificantBits();
        if (raw == Long.MIN_VALUE) {
            return 0L;
        }
        return Math.abs(raw);
    }
}