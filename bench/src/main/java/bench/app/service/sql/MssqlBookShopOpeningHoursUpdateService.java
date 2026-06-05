package bench.app.service.sql;

import bench.app.model.common.BookShopOpeningHoursUpdateResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Time;
import java.time.LocalTime;

@Service
public class MssqlBookShopOpeningHoursUpdateService {
    private static final String SELECT_CURRENT_MONDAY_HOURS_BY_SHOP_ID = """
            SELECT oh.opensAtMonday, oh.closesAtMonday
            FROM bench.BookShop bs
            JOIN bench.BookShopOpeningHours oh ON oh.id = bs.openingHoursId
            WHERE bs.id = ?
            """;

    private static final String UPDATE_MONDAY_HOURS_BY_SHOP_ID = """
            UPDATE oh
            SET opensAtMonday = ?, closesAtMonday = ?
            FROM bench.BookShopOpeningHours oh
            JOIN bench.BookShop bs ON bs.openingHoursId = oh.id
            WHERE bs.id = ?
            """;

    private final JdbcTemplate jdbcTemplate;

    public MssqlBookShopOpeningHoursUpdateService(@Qualifier("mssqlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public BookShopOpeningHoursUpdateResult updateMondayOpeningHours(
            long shopId,
            LocalTime opensAtMonday,
            LocalTime closesAtMonday,
            boolean restoreAfterUpdate
    ) {
        MondayHours previousHours = null;
        if (restoreAfterUpdate) {
            previousHours = jdbcTemplate.query(
                    SELECT_CURRENT_MONDAY_HOURS_BY_SHOP_ID,
                    rs -> rs.next()
                            ? new MondayHours(toLocalTime(rs.getTime("opensAtMonday")), toLocalTime(rs.getTime("closesAtMonday")))
                            : null,
                    shopId
            );

            if (previousHours == null) {
                throw new IllegalArgumentException("Nie znaleziono sklepu lub godzin otwarcia dla shopId=" + shopId);
            }
        }

        int affectedRows = jdbcTemplate.update(
                UPDATE_MONDAY_HOURS_BY_SHOP_ID,
                Time.valueOf(opensAtMonday),
                Time.valueOf(closesAtMonday),
                shopId
        );
        if (affectedRows == 0) {
            throw new IllegalArgumentException("Aktualizacja nie objęła żadnego rekordu dla shopId=" + shopId);
        }

        LocalTime finalOpensAtMonday = opensAtMonday;
        LocalTime finalClosesAtMonday = closesAtMonday;
        if (restoreAfterUpdate) {
            MondayHours nonNullPreviousHours = previousHours;
            jdbcTemplate.update(
                    UPDATE_MONDAY_HOURS_BY_SHOP_ID,
                    Time.valueOf(nonNullPreviousHours.opensAtMonday),
                    Time.valueOf(nonNullPreviousHours.closesAtMonday),
                    shopId
            );
            finalOpensAtMonday = nonNullPreviousHours.opensAtMonday;
            finalClosesAtMonday = nonNullPreviousHours.closesAtMonday;
        }

        return new BookShopOpeningHoursUpdateResult(
                shopId,
                previousHours == null ? null : previousHours.opensAtMonday,
                previousHours == null ? null : previousHours.closesAtMonday,
                opensAtMonday,
                closesAtMonday,
                finalOpensAtMonday,
                finalClosesAtMonday,
                restoreAfterUpdate,
                affectedRows
        );
    }

    private static LocalTime toLocalTime(Time value) {
        return value == null ? null : value.toLocalTime();
    }

    private record MondayHours(LocalTime opensAtMonday, LocalTime closesAtMonday) {
    }
}
