package bench.app.service.userpermission;

import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class UserPermissionQueryCatalog {
    private final Map<DatabaseEngine, Map<UserPermissionQueryType, String>> predefinedQueries;

    public UserPermissionQueryCatalog() {
        this.predefinedQueries = new EnumMap<>(DatabaseEngine.class);

        Map<UserPermissionQueryType, String> relationalQueries = new EnumMap<>(UserPermissionQueryType.class);

        // CREATE 1: uprawnienia konta uzytkownika.
        // SELECT_MAX_ID wyznacza kolejne id, CREATE wstawia rekord (id, permission, details).
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.useraccountpermissions");
        relationalQueries.put(UserPermissionQueryType.CREATE,
                "INSERT INTO bench.useraccountpermissions (id, permission, details) VALUES (?, ?, ?)");

        // CREATE 2: metoda wypozyczenia (np. punkt odbioru).
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_RENTAL_METHOD_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookrentalmethod");
        relationalQueries.put(UserPermissionQueryType.CREATE_RENTAL_METHOD,
                "INSERT INTO bench.bookrentalmethod (id, method) VALUES (?, ?)");

        // CREATE 3: sklep + godziny otwarcia.
        // Kolejnosc: tworzymy BookShop (openinghoursid = NULL), tworzymy BookShopOpeningHours,
        // a na koncu aktualizujemy BookShop o docelowe openinghoursid.
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_BOOKSHOP_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookshop");
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_OPENING_HOURS_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookshopopeninghours");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_EMPLOYEE_ID,
                "SELECT MIN(id) FROM bench.employee");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOKSHOP,
                "INSERT INTO bench.bookshop (id, shopname, address, email, managerid, openinghoursid) VALUES (?, ?, ?, ?, ?, NULL)");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOKSHOP_OPENING_HOURS,
                "INSERT INTO bench.bookshopopeninghours (id, opensatmonday, closesatmonday, opensattuesday, closesattuesday, opensatwednesday, closesatwednesday, opensatthursday, closesatthursday, opensatfriday, closesatfriday, opensatsaturday, closesatsaturday, opensatsunday, closesatsunday, bookshopid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.UPDATE_BOOKSHOP_OPENING_HOURS_ID,
                "UPDATE bench.bookshop SET openinghoursid = ? WHERE id = ?");

        // CREATE 4: rejestracja uzytkownika (3 tabele z tym samym id logicznym).
        // bookshopuser -> usercard -> useraccount.
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_BOOKSHOP_USER_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookshopuser");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOKSHOP_USER,
                "INSERT INTO bench.bookshopuser (id, name, surname, phonenumber, email, mainbookshopid, isactiveid) VALUES (?, ?, ?, ?, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_USER_CARD_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.usercard");
        relationalQueries.put(UserPermissionQueryType.CREATE_USER_CARD,
                "INSERT INTO bench.usercard (id, cardidnumber, userid, isactiveid) VALUES (?, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_USER_ACCOUNT_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.useraccount");
        relationalQueries.put(UserPermissionQueryType.CREATE_USER_ACCOUNT,
                "INSERT INTO bench.useraccount (id, login, passwordhash, userid, permissionsid) VALUES (?, ?, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_ACTIVATION_STATUS_ID,
                "SELECT MIN(id) FROM bench.activationstatus");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_PERMISSION_ID,
                "SELECT MIN(id) FROM bench.useraccountpermissions");

        // CREATE 5: ksiazka + powiazanie ksiazki z oferta sklepu.
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_BOOK_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.book");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOK,
                "INSERT INTO bench.book (id, author, title, isInReadingRoom, bookShopId) VALUES (?, ?, ?, 0, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_BOOKSHOP_OFFERING_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookshopoffering");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOKSHOP_OFFERING,
                "INSERT INTO bench.bookshopoffering (id, bookid, bookshopid) VALUES (?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_BOOK_ID,
                "SELECT MIN(id) FROM bench.book");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_BOOKSHOP_ID,
                "SELECT MIN(id) FROM bench.bookshop");

        // CREATE 6: rezerwacja ksiazki przez uzytkownika.
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_BOOK_RESERVATION_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookreservation");
        relationalQueries.put(UserPermissionQueryType.CREATE_BOOK_RESERVATION,
                "INSERT INTO bench.bookreservation (id, bookid, userid, whenreserved) VALUES (?, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_BOOKSHOP_USER_ID,
                "SELECT MIN(id) FROM bench.bookshopuser");

        // CREATE 7: pelne wypozyczenie.
        // Placeholdery dla CREATE_RENTAL_FULL:
        // 1:id, 2:bookid, 3:userid, 4:employeeid, 5:bookshopid, 6:rentalMethodId, 7:startdate, 8:enddate.
        // isreturned jest ustawiane stale na 0 (false).
        relationalQueries.put(UserPermissionQueryType.SELECT_MAX_RENTAL_ID,
                "SELECT COALESCE(MAX(id), 0) FROM bench.bookrental");
        relationalQueries.put(UserPermissionQueryType.CREATE_RENTAL_FULL,
                "INSERT INTO bench.bookrental (id, bookid, userid, employeeid, bookshopid, isreturned, rentalMethodId, startdate, enddate) VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.SELECT_ANY_RENTAL_METHOD_ID,
                "SELECT MIN(id) FROM bench.bookrentalmethod");

        // CREATE 8: wypozyczenie warunkowe.
        // Najpierw probujemy znalezc aktywnego uzytkownika, ktory ma ksiazke w swoim sklepie.
        // Gdy nie ma wyniku, logika serwisu robi fallback do dowolnego uzytkownika.
        relationalQueries.put(UserPermissionQueryType.SELECT_ACTIVE_USER_WITH_BOOK_IN_SHOP,
                "SELECT u.id FROM bench.bookshopuser u JOIN bench.book b ON u.mainbookshopid = b.bookshopid WHERE u.isactiveid IS NOT NULL ORDER BY u.id LIMIT 1");
        relationalQueries.put(UserPermissionQueryType.CREATE_RENTAL_CONDITIONAL,
                "INSERT INTO bench.bookrental (id, bookid, userid, employeeid, bookshopid, isreturned, rentalMethodId, startdate, enddate) VALUES (?, ?, ?, ?, ?, 0, ?, ?, ?)");
        relationalQueries.put(UserPermissionQueryType.CREATE_BATCH_SUPPLY,
                "INSERT INTO bench.book (id, author, title, isInReadingRoom, bookShopId) VALUES (?, ?, ?, 0, ?)");

        // Operacje bazowe CRUD dla benchmarku READ/UPDATE/DELETE na useraccountpermissions.
        relationalQueries.put(UserPermissionQueryType.READ,
                "SELECT id, permission, details FROM bench.useraccountpermissions WHERE id = ?");
        relationalQueries.put(UserPermissionQueryType.UPDATE,
                "UPDATE bench.useraccountpermissions SET permission = ?, details = ? WHERE id = ?");
        relationalQueries.put(UserPermissionQueryType.DELETE,
                "DELETE FROM bench.useraccountpermissions WHERE id = ?");

        predefinedQueries.put(DatabaseEngine.POSTGRESQL, relationalQueries);
        predefinedQueries.put(DatabaseEngine.MSSQL, relationalQueries);
        predefinedQueries.put(DatabaseEngine.CASSANDRA, Map.of());
        predefinedQueries.put(DatabaseEngine.SCYLLA, Map.of());
    }

    public String getRequiredQuery(DatabaseEngine engine, UserPermissionQueryType queryType) {
        Map<UserPermissionQueryType, String> engineQueries = predefinedQueries.get(engine);
        if (engineQueries == null || !engineQueries.containsKey(queryType)) {
            throw new IllegalArgumentException(
                    "Brak predefiniowanego zapytania " + queryType + " dla silnika " + engine.propertyValue()
            );
        }
        return engineQueries.get(queryType);
    }
}
