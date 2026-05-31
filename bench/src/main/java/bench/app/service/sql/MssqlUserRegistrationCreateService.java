package bench.app.service.sql;

import bench.app.model.common.UserRegistrationCreateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

@Service
public class MssqlUserRegistrationCreateService {
    private static final String SELECT_NEXT_USER_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.BookShopUser
            """;

    private static final String SELECT_NEXT_USER_CARD_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.UserCard
            """;

    private static final String SELECT_NEXT_USER_ACCOUNT_ID = """
            SELECT ISNULL(MAX(id), 0) + 1 AS next_id
            FROM bench.UserAccount
            """;

    private static final String SELECT_ACTIVE_STATUS_ID = """
            SELECT TOP 1 id
            FROM bench.ActivationStatus
            WHERE UPPER(status) = 'ACTIVE'
            ORDER BY id
            """;

    private static final String SELECT_FALLBACK_STATUS_ID = """
            SELECT TOP 1 id
            FROM bench.ActivationStatus
            ORDER BY id
            """;

    private static final String SELECT_DEFAULT_SHOP_ID = """
            SELECT TOP 1 id
            FROM bench.BookShop
            ORDER BY id
            """;

    private static final String SELECT_DEFAULT_PERMISSION_ID = """
            SELECT TOP 1 id
            FROM bench.UserAccountPermissions
            ORDER BY id
            """;

    private static final String INSERT_USER = """
            INSERT INTO bench.BookShopUser (id, name, surname, phoneNumber, email, mainBookShopId, isActiveId)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

    private static final String INSERT_USER_CARD = """
            INSERT INTO bench.UserCard (id, cardIdNumber, userId, isActiveId)
            VALUES (?, ?, ?, ?)
            """;

    private static final String INSERT_USER_ACCOUNT = """
            INSERT INTO bench.UserAccount (id, login, passwordHash, userId, permissionsId)
            VALUES (?, ?, ?, ?, ?)
            """;

    private static final String DELETE_USER_ACCOUNT = """
            DELETE FROM bench.UserAccount
            WHERE id = ?
            """;

    private static final String DELETE_USER_CARD = """
            DELETE FROM bench.UserCard
            WHERE id = ?
            """;

    private static final String DELETE_USER = """
            DELETE FROM bench.BookShopUser
            WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlUserRegistrationCreateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Transactional(transactionManager = "mssqlTransactionManager")
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

        Long userId = queryLong(SELECT_NEXT_USER_ID, "Nie udało się ustalić nowego id użytkownika");
        Long userCardId = queryLong(SELECT_NEXT_USER_CARD_ID, "Nie udało się ustalić nowego id karty użytkownika");
        Long userAccountId = queryLong(SELECT_NEXT_USER_ACCOUNT_ID, "Nie udało się ustalić nowego id konta użytkownika");
        Long activeStatusId = queryOptionalLong(SELECT_ACTIVE_STATUS_ID);
        if (activeStatusId == null) {
            activeStatusId = queryLong(SELECT_FALLBACK_STATUS_ID, "Brak statusu aktywacji w tabeli ActivationStatus");
        }
        Long defaultShopId = queryLong(SELECT_DEFAULT_SHOP_ID, "Brak sklepu referencyjnego w tabeli BookShop");
        Long defaultPermissionId = queryLong(SELECT_DEFAULT_PERMISSION_ID, "Brak uprawnienia w tabeli UserAccountPermissions");

        int insertedRows = 0;
        insertedRows += jdbcTemplate.update(
                INSERT_USER,
                userId,
                name,
                surname,
                blankToNull(phoneNumber),
                email,
                defaultShopId,
                activeStatusId
        );

        String cardIdNumber = buildCardIdNumber(userId);
        insertedRows += jdbcTemplate.update(
                INSERT_USER_CARD,
                userCardId,
                cardIdNumber,
                userId,
                activeStatusId
        );

        insertedRows += jdbcTemplate.update(
                INSERT_USER_ACCOUNT,
                userAccountId,
                login,
                passwordHash,
                userId,
                defaultPermissionId
        );

        int deletedRows = 0;
        boolean existsAfterOperation = true;
        if (restoreAfterCreate) {
            deletedRows += jdbcTemplate.update(DELETE_USER_ACCOUNT, userAccountId);
            deletedRows += jdbcTemplate.update(DELETE_USER_CARD, userCardId);
            deletedRows += jdbcTemplate.update(DELETE_USER, userId);
            existsAfterOperation = false;
        }

        return new UserRegistrationCreateResult(
                userId,
                userCardId,
                userAccountId,
                login,
                email,
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
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

    private Long queryLong(String sql, String errorMessage) {
        Long value = queryOptionalLong(sql);
        if (value == null) {
            throw new IllegalArgumentException(errorMessage);
        }
        return value;
    }

    private Long queryOptionalLong(String sql) {
        return jdbcTemplate.query(sql, rs -> rs.next() ? rs.getLong(1) : null);
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String buildCardIdNumber(long userId) {
        return String.format("CARD-%024d", userId);
    }
}