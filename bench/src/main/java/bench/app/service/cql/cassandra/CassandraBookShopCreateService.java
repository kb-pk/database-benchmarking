package bench.app.service.cql.cassandra;

import bench.app.model.common.BookShopCreateResult;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CassandraBookShopCreateService {
    private static final String INSERT_BOOKSHOP = """
            INSERT INTO bookshops (
                shop_id, shop_name, address, email, manager_id,
                opens_at_monday, closes_at_monday,
                opens_at_tuesday, closes_at_tuesday,
                opens_at_wednesday, closes_at_wednesday,
                opens_at_thursday, closes_at_thursday,
                opens_at_friday, closes_at_friday,
                opens_at_saturday, closes_at_saturday,
                opens_at_sunday, closes_at_sunday
            ) VALUES (?, ?, ?, ?, ?, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
            """;

    private static final String DELETE_BOOKSHOP_BY_ID = "DELETE FROM bookshops WHERE shop_id = ?";

    private final CqlSession cassandraSession;

    public CassandraBookShopCreateService(@Qualifier("cassandraSession") CqlSession cassandraSession) {
        this.cassandraSession = cassandraSession;
    }

    public BookShopCreateResult createBookShop(String shopName, String address, String email, UUID managerId, boolean restoreAfterCreate) {
        if (shopName == null || shopName.isBlank()) {
            throw new IllegalArgumentException("shopName nie może być puste");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address nie może być puste");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email nie może być pusty");
        }

        UUID createdBookShopId = UUID.randomUUID();
        int insertedRows = cassandraSession.execute(
                SimpleStatement.builder(INSERT_BOOKSHOP)
                        .addPositionalValues(
                                createdBookShopId,
                                shopName,
                                address,
                                email,
                                managerId
                        )
                        .build()
        ).wasApplied() ? 1 : 0;

        int deletedRows = 0;
        boolean existsAfterOperation = true;

        if (restoreAfterCreate) {
            deletedRows = cassandraSession.execute(
                    SimpleStatement.builder(DELETE_BOOKSHOP_BY_ID)
                            .addPositionalValue(createdBookShopId)
                            .build()
            ).wasApplied() ? 1 : 0;
            existsAfterOperation = false;
        }

        return new BookShopCreateResult(
                createdBookShopId.toString(),
                shopName,
                address,
                email,
                managerId == null ? null : managerId.toString(),
                restoreAfterCreate,
                existsAfterOperation,
                insertedRows,
                deletedRows
        );
    }
}