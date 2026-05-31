package bench.app.controller;

import bench.app.model.common.Book;
import bench.app.model.common.BookDeliveryCreateResult;
import bench.app.model.common.BookRentalConditionalCreateResult;
import bench.app.model.common.BookRentalDeleteResult;
import bench.app.model.common.BookShopOfferingDeleteByUserResult;
import bench.app.model.common.BookReservationBulkDeleteResult;
import bench.app.model.common.EmployeeRentalDayDeleteResult;
import bench.app.model.common.BookReservationCreateResult;
import bench.app.model.common.BookReservationDeleteResult;
import bench.app.model.common.BookShopCreateResult;
import bench.app.model.common.BookRentalCloseOverdueResult;
import bench.app.model.common.BookListItem;
import bench.app.model.common.BookShopOpeningHoursUpdateResult;
import bench.app.model.common.BookShop;
import bench.app.service.cql.cassandra.CassandraAnalyticsService;
import bench.app.service.cql.cassandra.CassandraBookDeliveryCreateService;
import bench.app.service.cql.cassandra.CassandraBookRentalDeleteService;
import bench.app.service.cql.cassandra.CassandraBookRentalConditionalCreateService;
import bench.app.service.cql.cassandra.CassandraBookReservationBulkDeleteService;
import bench.app.service.cql.cassandra.CassandraBookReservationDeleteService;
import bench.app.service.cql.cassandra.CassandraBookShopService;
import bench.app.service.cql.cassandra.CassandraBookShopCreateService;
import bench.app.service.cql.cassandra.CassandraBookShopOfferingDeleteService;
import bench.app.service.cql.cassandra.CassandraEmployeeRentalDayDeleteService;
import bench.app.service.cql.cassandra.CassandraBookReservationCreateService;
import bench.app.service.cql.scylla.ScyllaAnalyticsService;
import bench.app.service.cql.scylla.ScyllaBookDeliveryCreateService;
import bench.app.service.cql.scylla.ScyllaBookRentalDeleteService;
import bench.app.service.cql.scylla.ScyllaBookRentalConditionalCreateService;
import bench.app.service.cql.scylla.ScyllaBookReservationBulkDeleteService;
import bench.app.service.cql.scylla.ScyllaBookReservationDeleteService;
import bench.app.service.cql.scylla.ScyllaBookShopService;
import bench.app.service.cql.scylla.ScyllaBookShopCreateService;
import bench.app.service.cql.scylla.ScyllaBookShopOfferingDeleteService;
import bench.app.service.cql.scylla.ScyllaEmployeeRentalDayDeleteService;
import bench.app.service.cql.scylla.ScyllaBookReservationCreateService;
import bench.app.service.sql.MssqlBookShopService;
import bench.app.service.sql.MssqlBookDeliveryCreateService;
import bench.app.service.sql.MssqlBookShopCreateService;
import bench.app.service.sql.MssqlBookRentalConditionalCreateService;
import bench.app.service.sql.MssqlBookRentalDeleteService;
import bench.app.service.sql.MssqlBookShopOfferingDeleteService;
import bench.app.service.sql.MssqlEmployeeRentalDayDeleteService;
import bench.app.service.sql.MssqlBookReservationBulkDeleteService;
import bench.app.service.sql.MssqlBookReservationCreateService;
import bench.app.service.sql.PostgresBookShopService;
import bench.app.service.sql.PostgresBookDeliveryCreateService;
import bench.app.service.sql.PostgresBookShopCreateService;
import bench.app.service.sql.PostgresBookRentalConditionalCreateService;
import bench.app.service.sql.PostgresBookRentalDeleteService;
import bench.app.service.sql.PostgresBookShopOfferingDeleteService;
import bench.app.service.sql.PostgresEmployeeRentalDayDeleteService;
import bench.app.service.sql.PostgresBookReservationBulkDeleteService;
import bench.app.service.sql.PostgresBookReservationCreateService;
import bench.app.model.common.EmployeeRentalCount;
import bench.app.service.sql.PostgresBookRentalStatsService;
import bench.app.service.sql.MssqlBookRentalStatsService;
import bench.app.service.sql.PostgresBookRentalRankingService;
import bench.app.service.sql.MssqlBookRentalRankingService;
import bench.app.service.sql.PostgresBookRentalUpdateService;
import bench.app.service.sql.PostgresBookShopOpeningHoursUpdateService;
import bench.app.service.sql.MssqlBookRentalUpdateService;
import bench.app.service.sql.MssqlBookShopOpeningHoursUpdateService;
import bench.app.model.common.BookRentalRanking;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookshop")
public class BookShopController {
    private final PostgresBookShopService postgresService;
    private final MssqlBookShopService mssqlService;
    private final PostgresBookDeliveryCreateService postgresBookDeliveryCreateService;
    private final MssqlBookDeliveryCreateService mssqlBookDeliveryCreateService;
    private final PostgresBookShopCreateService postgresBookShopCreateService;
    private final MssqlBookShopCreateService mssqlBookShopCreateService;
    private final PostgresBookReservationCreateService postgresBookReservationCreateService;
    private final MssqlBookReservationCreateService mssqlBookReservationCreateService;
    private final PostgresBookReservationBulkDeleteService postgresBookReservationBulkDeleteService;
    private final MssqlBookReservationBulkDeleteService mssqlBookReservationBulkDeleteService;
    private final CassandraBookReservationBulkDeleteService cassandraBookReservationBulkDeleteService;
    private final ScyllaBookReservationBulkDeleteService scyllaBookReservationBulkDeleteService;
    private final CassandraBookReservationCreateService cassandraBookReservationCreateService;
    private final ScyllaBookReservationCreateService scyllaBookReservationCreateService;
    private final CassandraBookReservationDeleteService cassandraBookReservationDeleteService;
    private final ScyllaBookReservationDeleteService scyllaBookReservationDeleteService;
    private final PostgresBookRentalConditionalCreateService postgresBookRentalConditionalCreateService;
    private final MssqlBookRentalConditionalCreateService mssqlBookRentalConditionalCreateService;
    private final PostgresBookRentalDeleteService postgresBookRentalDeleteService;
    private final MssqlBookRentalDeleteService mssqlBookRentalDeleteService;
    private final CassandraBookRentalDeleteService cassandraBookRentalDeleteService;
    private final ScyllaBookRentalDeleteService scyllaBookRentalDeleteService;
    private final PostgresBookShopOfferingDeleteService postgresBookShopOfferingDeleteService;
    private final MssqlBookShopOfferingDeleteService mssqlBookShopOfferingDeleteService;
    private final CassandraBookShopOfferingDeleteService cassandraBookShopOfferingDeleteService;
    private final ScyllaBookShopOfferingDeleteService scyllaBookShopOfferingDeleteService;
    private final PostgresEmployeeRentalDayDeleteService postgresEmployeeRentalDayDeleteService;
    private final MssqlEmployeeRentalDayDeleteService mssqlEmployeeRentalDayDeleteService;
    private final CassandraEmployeeRentalDayDeleteService cassandraEmployeeRentalDayDeleteService;
    private final ScyllaEmployeeRentalDayDeleteService scyllaEmployeeRentalDayDeleteService;
    private final CassandraBookRentalConditionalCreateService cassandraBookRentalConditionalCreateService;
    private final ScyllaBookRentalConditionalCreateService scyllaBookRentalConditionalCreateService;
    private final CassandraBookDeliveryCreateService cassandraBookDeliveryCreateService;
    private final ScyllaBookDeliveryCreateService scyllaBookDeliveryCreateService;
    private final CassandraBookShopService cassandraService;
    private final ScyllaBookShopService scyllaService;
    private final CassandraBookShopCreateService cassandraBookShopCreateService;
    private final ScyllaBookShopCreateService scyllaBookShopCreateService;

    private final bench.app.service.sql.PostgresBookRentalService postgresBookRentalService;
    private final bench.app.service.sql.MssqlBookRentalService mssqlBookRentalService;
    private final bench.app.service.sql.PostgresBookReservationService postgresBookReservationService;
    private final bench.app.service.sql.MssqlBookReservationService mssqlBookReservationService;

    private final PostgresBookRentalStatsService postgresBookRentalStatsService;
    private final MssqlBookRentalStatsService mssqlBookRentalStatsService;

    private final PostgresBookRentalRankingService postgresBookRentalRankingService;
    private final MssqlBookRentalRankingService mssqlBookRentalRankingService;
    private final CassandraAnalyticsService cassandraAnalyticsService;
    private final ScyllaAnalyticsService scyllaAnalyticsService;
    private final PostgresBookShopOpeningHoursUpdateService postgresBookShopOpeningHoursUpdateService;
    private final MssqlBookShopOpeningHoursUpdateService mssqlBookShopOpeningHoursUpdateService;
    private final PostgresBookRentalUpdateService postgresBookRentalUpdateService;
    private final MssqlBookRentalUpdateService mssqlBookRentalUpdateService;

    public BookShopController(
            PostgresBookShopService postgresService,
            MssqlBookShopService mssqlService,
            PostgresBookDeliveryCreateService postgresBookDeliveryCreateService,
            MssqlBookDeliveryCreateService mssqlBookDeliveryCreateService,
            PostgresBookShopCreateService postgresBookShopCreateService,
            MssqlBookShopCreateService mssqlBookShopCreateService,
            PostgresBookReservationCreateService postgresBookReservationCreateService,
            MssqlBookReservationCreateService mssqlBookReservationCreateService,
            PostgresBookReservationBulkDeleteService postgresBookReservationBulkDeleteService,
            MssqlBookReservationBulkDeleteService mssqlBookReservationBulkDeleteService,
            CassandraBookReservationBulkDeleteService cassandraBookReservationBulkDeleteService,
            ScyllaBookReservationBulkDeleteService scyllaBookReservationBulkDeleteService,
            CassandraBookReservationCreateService cassandraBookReservationCreateService,
            ScyllaBookReservationCreateService scyllaBookReservationCreateService,
            CassandraBookReservationDeleteService cassandraBookReservationDeleteService,
            ScyllaBookReservationDeleteService scyllaBookReservationDeleteService,
            PostgresBookRentalConditionalCreateService postgresBookRentalConditionalCreateService,
            MssqlBookRentalConditionalCreateService mssqlBookRentalConditionalCreateService,
            PostgresBookRentalDeleteService postgresBookRentalDeleteService,
            MssqlBookRentalDeleteService mssqlBookRentalDeleteService,
            CassandraBookRentalDeleteService cassandraBookRentalDeleteService,
            ScyllaBookRentalDeleteService scyllaBookRentalDeleteService,
            PostgresBookShopOfferingDeleteService postgresBookShopOfferingDeleteService,
            MssqlBookShopOfferingDeleteService mssqlBookShopOfferingDeleteService,
            CassandraBookShopOfferingDeleteService cassandraBookShopOfferingDeleteService,
            ScyllaBookShopOfferingDeleteService scyllaBookShopOfferingDeleteService,
            PostgresEmployeeRentalDayDeleteService postgresEmployeeRentalDayDeleteService,
            MssqlEmployeeRentalDayDeleteService mssqlEmployeeRentalDayDeleteService,
            CassandraEmployeeRentalDayDeleteService cassandraEmployeeRentalDayDeleteService,
            ScyllaEmployeeRentalDayDeleteService scyllaEmployeeRentalDayDeleteService,
            CassandraBookRentalConditionalCreateService cassandraBookRentalConditionalCreateService,
            ScyllaBookRentalConditionalCreateService scyllaBookRentalConditionalCreateService,
            CassandraBookDeliveryCreateService cassandraBookDeliveryCreateService,
            ScyllaBookDeliveryCreateService scyllaBookDeliveryCreateService,
            CassandraBookShopService cassandraService,
            ScyllaBookShopService scyllaService,
            CassandraBookShopCreateService cassandraBookShopCreateService,
            ScyllaBookShopCreateService scyllaBookShopCreateService,
            bench.app.service.sql.PostgresBookRentalService postgresBookRentalService,
            bench.app.service.sql.MssqlBookRentalService mssqlBookRentalService,
            bench.app.service.sql.PostgresBookReservationService postgresBookReservationService,
            bench.app.service.sql.MssqlBookReservationService mssqlBookReservationService,
            PostgresBookRentalStatsService postgresBookRentalStatsService,
            MssqlBookRentalStatsService mssqlBookRentalStatsService,
            PostgresBookRentalRankingService postgresBookRentalRankingService,
            MssqlBookRentalRankingService mssqlBookRentalRankingService,
            CassandraAnalyticsService cassandraAnalyticsService,
            ScyllaAnalyticsService scyllaAnalyticsService,
            PostgresBookShopOpeningHoursUpdateService postgresBookShopOpeningHoursUpdateService,
            MssqlBookShopOpeningHoursUpdateService mssqlBookShopOpeningHoursUpdateService,
            PostgresBookRentalUpdateService postgresBookRentalUpdateService,
            MssqlBookRentalUpdateService mssqlBookRentalUpdateService) {
        this.postgresService = postgresService;
        this.mssqlService = mssqlService;
        this.postgresBookDeliveryCreateService = postgresBookDeliveryCreateService;
        this.mssqlBookDeliveryCreateService = mssqlBookDeliveryCreateService;
        this.postgresBookShopCreateService = postgresBookShopCreateService;
        this.mssqlBookShopCreateService = mssqlBookShopCreateService;
        this.postgresBookReservationCreateService = postgresBookReservationCreateService;
        this.mssqlBookReservationCreateService = mssqlBookReservationCreateService;
        this.postgresBookReservationBulkDeleteService = postgresBookReservationBulkDeleteService;
        this.mssqlBookReservationBulkDeleteService = mssqlBookReservationBulkDeleteService;
        this.cassandraBookReservationBulkDeleteService = cassandraBookReservationBulkDeleteService;
        this.scyllaBookReservationBulkDeleteService = scyllaBookReservationBulkDeleteService;
        this.cassandraBookReservationCreateService = cassandraBookReservationCreateService;
        this.scyllaBookReservationCreateService = scyllaBookReservationCreateService;
        this.cassandraBookReservationDeleteService = cassandraBookReservationDeleteService;
        this.scyllaBookReservationDeleteService = scyllaBookReservationDeleteService;
        this.postgresBookRentalConditionalCreateService = postgresBookRentalConditionalCreateService;
        this.mssqlBookRentalConditionalCreateService = mssqlBookRentalConditionalCreateService;
        this.postgresBookRentalDeleteService = postgresBookRentalDeleteService;
        this.mssqlBookRentalDeleteService = mssqlBookRentalDeleteService;
        this.cassandraBookRentalDeleteService = cassandraBookRentalDeleteService;
        this.scyllaBookRentalDeleteService = scyllaBookRentalDeleteService;
        this.postgresBookShopOfferingDeleteService = postgresBookShopOfferingDeleteService;
        this.mssqlBookShopOfferingDeleteService = mssqlBookShopOfferingDeleteService;
        this.cassandraBookShopOfferingDeleteService = cassandraBookShopOfferingDeleteService;
        this.scyllaBookShopOfferingDeleteService = scyllaBookShopOfferingDeleteService;
        this.postgresEmployeeRentalDayDeleteService = postgresEmployeeRentalDayDeleteService;
        this.mssqlEmployeeRentalDayDeleteService = mssqlEmployeeRentalDayDeleteService;
        this.cassandraEmployeeRentalDayDeleteService = cassandraEmployeeRentalDayDeleteService;
        this.scyllaEmployeeRentalDayDeleteService = scyllaEmployeeRentalDayDeleteService;
        this.cassandraBookRentalConditionalCreateService = cassandraBookRentalConditionalCreateService;
        this.scyllaBookRentalConditionalCreateService = scyllaBookRentalConditionalCreateService;
        this.cassandraBookDeliveryCreateService = cassandraBookDeliveryCreateService;
        this.scyllaBookDeliveryCreateService = scyllaBookDeliveryCreateService;
        this.cassandraService = cassandraService;
        this.scyllaService = scyllaService;
        this.cassandraBookShopCreateService = cassandraBookShopCreateService;
        this.scyllaBookShopCreateService = scyllaBookShopCreateService;
        this.postgresBookRentalService = postgresBookRentalService;
        this.mssqlBookRentalService = mssqlBookRentalService;
        this.postgresBookReservationService = postgresBookReservationService;
        this.mssqlBookReservationService = mssqlBookReservationService;
        this.postgresBookRentalStatsService = postgresBookRentalStatsService;
        this.mssqlBookRentalStatsService = mssqlBookRentalStatsService;
        this.postgresBookRentalRankingService = postgresBookRentalRankingService;
        this.mssqlBookRentalRankingService = mssqlBookRentalRankingService;
        this.cassandraAnalyticsService = cassandraAnalyticsService;
        this.scyllaAnalyticsService = scyllaAnalyticsService;
        this.postgresBookShopOpeningHoursUpdateService = postgresBookShopOpeningHoursUpdateService;
        this.mssqlBookShopOpeningHoursUpdateService = mssqlBookShopOpeningHoursUpdateService;
        this.postgresBookRentalUpdateService = postgresBookRentalUpdateService;
        this.mssqlBookRentalUpdateService = mssqlBookRentalUpdateService;
    }

    // C4: Dodanie rezerwacji książki przez użytkownika (z kontrolą istnienia)
    @PostMapping("/reservations/create")
    public BookReservationCreateResult createBookReservation(
            @RequestParam String db,
            @RequestParam String bookId,
            @RequestParam String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate whenReserved,
            @RequestParam(defaultValue = "true") boolean restoreAfterCreate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookReservationCreateService
                .createReservation(Long.parseLong(bookId), Long.parseLong(userId), whenReserved, restoreAfterCreate);
            case "MSSQL" -> this.mssqlBookReservationCreateService
                .createReservation(Long.parseLong(bookId), Long.parseLong(userId), whenReserved, restoreAfterCreate);
            case "CASSANDRA" -> this.cassandraBookReservationCreateService
                .createReservation(UUID.fromString(bookId), UUID.fromString(userId), whenReserved, restoreAfterCreate);
            case "SCYLLA" -> this.scyllaBookReservationCreateService
                .createReservation(UUID.fromString(bookId), UUID.fromString(userId), whenReserved, restoreAfterCreate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

    // D1: Usunięcie jednej rezerwacji książki w silnikach SQL
    @PostMapping("/reservations/delete")
        public BookReservationDeleteResult deleteBookReservation(
            @RequestParam String db,
            @RequestParam String reservationId,
            @RequestParam(defaultValue = "true") boolean restoreAfterDelete
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookReservationCreateService
                .deleteReservation(Long.parseLong(reservationId), restoreAfterDelete);
            case "MSSQL" -> this.mssqlBookReservationCreateService
                .deleteReservation(Long.parseLong(reservationId), restoreAfterDelete);
            case "CASSANDRA" -> this.cassandraBookReservationDeleteService
                .deleteReservation(UUID.fromString(reservationId), restoreAfterDelete);
            case "SCYLLA" -> this.scyllaBookReservationDeleteService
                .deleteReservation(UUID.fromString(reservationId), restoreAfterDelete);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

    // D2: Usunięcie pojedynczego wypożyczenia w silnikach SQL
    @PostMapping("/rentals/delete")
        public BookRentalDeleteResult deleteBookRental(
            @RequestParam String db,
            @RequestParam String rentalId,
            @RequestParam(defaultValue = "true") boolean restoreAfterDelete
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookRentalDeleteService
                .deleteRental(Long.parseLong(rentalId), restoreAfterDelete);
            case "MSSQL" -> this.mssqlBookRentalDeleteService
                .deleteRental(Long.parseLong(rentalId), restoreAfterDelete);
            case "CASSANDRA" -> this.cassandraBookRentalDeleteService
                .deleteRental(UUID.fromString(rentalId), restoreAfterDelete);
            case "SCYLLA" -> this.scyllaBookRentalDeleteService
                .deleteRental(UUID.fromString(rentalId), restoreAfterDelete);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

    // D3: Masowe czyszczenie starych rezerwacji bez finalizacji
    @PostMapping("/reservations/cleanup-old-unfinalized")
    public BookReservationBulkDeleteResult deleteOldUnfinalizedReservations(
            @RequestParam String db,
            @RequestParam(defaultValue = "2") int monthsThreshold,
            @RequestParam(defaultValue = "true") boolean restoreAfterDelete
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookReservationBulkDeleteService
                    .deleteOldUnfinalizedReservations(monthsThreshold, restoreAfterDelete);
            case "MSSQL" -> this.mssqlBookReservationBulkDeleteService
                    .deleteOldUnfinalizedReservations(monthsThreshold, restoreAfterDelete);
            case "CASSANDRA" -> this.cassandraBookReservationBulkDeleteService
                .deleteOldUnfinalizedReservations(monthsThreshold, restoreAfterDelete);
            case "SCYLLA" -> this.scyllaBookReservationBulkDeleteService
                .deleteOldUnfinalizedReservations(monthsThreshold, restoreAfterDelete);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

        // D5: Usunięcie z oferty książek stale wypożyczonych przez konkretnego użytkownika
        @PostMapping("/offerings/delete-permanently-borrowed-by-user")
        public BookShopOfferingDeleteByUserResult deleteOfferingsForUserPermanentlyBorrowedBooks(
            @RequestParam String db,
            @RequestParam String userId,
            @RequestParam(defaultValue = "true") boolean restoreAfterDelete
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookShopOfferingDeleteService
                .deleteOfferingsForUserPermanentlyBorrowedBooks(Long.parseLong(userId), restoreAfterDelete);
            case "MSSQL" -> this.mssqlBookShopOfferingDeleteService
                .deleteOfferingsForUserPermanentlyBorrowedBooks(Long.parseLong(userId), restoreAfterDelete);
            case "CASSANDRA" -> this.cassandraBookShopOfferingDeleteService
                .deleteOfferingsForUserPermanentlyBorrowedBooks(UUID.fromString(userId), restoreAfterDelete);
            case "SCYLLA" -> this.scyllaBookShopOfferingDeleteService
                .deleteOfferingsForUserPermanentlyBorrowedBooks(UUID.fromString(userId), restoreAfterDelete);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

            // D6: Usunięcie wszystkich wypożyczeń zrobionych przez danego pracownika danego dnia
            @PostMapping("/rentals/delete-by-employee-day")
            public EmployeeRentalDayDeleteResult deleteRentalsByEmployeeAndDay(
                @RequestParam String db,
                @RequestParam String employeeId,
                @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate rentalDate,
                @RequestParam(defaultValue = "true") boolean restoreAfterDelete
            ) {
            return switch (db) {
                case "POSTGRESQL" -> this.postgresEmployeeRentalDayDeleteService
                    .deleteRentalsByEmployeeAndDay(Long.parseLong(employeeId), rentalDate, restoreAfterDelete);
                case "MSSQL" -> this.mssqlEmployeeRentalDayDeleteService
                    .deleteRentalsByEmployeeAndDay(Long.parseLong(employeeId), rentalDate, restoreAfterDelete);
                case "CASSANDRA" -> this.cassandraEmployeeRentalDayDeleteService
                    .deleteRentalsByEmployeeAndDay(UUID.fromString(employeeId), rentalDate, restoreAfterDelete);
                case "SCYLLA" -> this.scyllaEmployeeRentalDayDeleteService
                    .deleteRentalsByEmployeeAndDay(UUID.fromString(employeeId), rentalDate, restoreAfterDelete);
                default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
            };
            }

    // C5: Warunkowe utworzenie wypożyczenia (użytkownik aktywny i książka należy do sklepu)
    @PostMapping("/rentals/create-conditional")
    public BookRentalConditionalCreateResult createConditionalBookRental(
            @RequestParam String db,
            @RequestParam String shopId,
            @RequestParam String bookId,
            @RequestParam String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "true") boolean restoreAfterCreate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookRentalConditionalCreateService
                .createRentalIfValid(Long.parseLong(shopId), Long.parseLong(bookId), Long.parseLong(userId), startDate, restoreAfterCreate);
            case "MSSQL" -> this.mssqlBookRentalConditionalCreateService
                .createRentalIfValid(Long.parseLong(shopId), Long.parseLong(bookId), Long.parseLong(userId), startDate, restoreAfterCreate);
            case "CASSANDRA" -> this.cassandraBookRentalConditionalCreateService
                .createRentalIfValid(UUID.fromString(shopId), UUID.fromString(bookId), UUID.fromString(userId), startDate, restoreAfterCreate);
            case "SCYLLA" -> this.scyllaBookRentalConditionalCreateService
                .createRentalIfValid(UUID.fromString(shopId), UUID.fromString(bookId), UUID.fromString(userId), startDate, restoreAfterCreate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

    // C6: Zdarzenie "nowa dostawa do sklepu" (batch książek + oferta)
    @PostMapping("/{shopId}/delivery/new-batch")
    public BookDeliveryCreateResult createNewBookDelivery(
            @RequestParam String db,
            @PathVariable("shopId") String shopId,
            @RequestParam(defaultValue = "20") int batchSize,
            @RequestParam(defaultValue = "true") boolean restoreAfterCreate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookDeliveryCreateService
                .createDelivery(Long.parseLong(shopId), batchSize, restoreAfterCreate);
            case "MSSQL" -> this.mssqlBookDeliveryCreateService
                .createDelivery(Long.parseLong(shopId), batchSize, restoreAfterCreate);
            case "CASSANDRA" -> this.cassandraBookDeliveryCreateService
                .createDelivery(UUID.fromString(shopId), batchSize, restoreAfterCreate);
            case "SCYLLA" -> this.scyllaBookDeliveryCreateService
                .createDelivery(UUID.fromString(shopId), batchSize, restoreAfterCreate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

        // C2: Dodanie nowego sklepu z przywróceniem poprzedniego stanu
        @PostMapping("/create")
        public BookShopCreateResult createBookShop(
            @RequestParam String db,
            @RequestParam String shopName,
            @RequestParam String address,
            @RequestParam String email,
            @RequestParam String managerId,
            @RequestParam(defaultValue = "true") boolean restoreAfterCreate
        ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookShopCreateService
                .createBookShop(shopName, address, email, Long.parseLong(managerId), restoreAfterCreate);
            case "MSSQL" -> this.mssqlBookShopCreateService
                .createBookShop(shopName, address, email, Long.parseLong(managerId), restoreAfterCreate);
            case "CASSANDRA" -> this.cassandraBookShopCreateService
                .createBookShop(shopName, address, email, UUID.fromString(managerId), restoreAfterCreate);
            case "SCYLLA" -> this.scyllaBookShopCreateService
                .createBookShop(shopName, address, email, UUID.fromString(managerId), restoreAfterCreate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
        }

    // U2: Aktualizacja godzin otwarcia sklepu (poniedziałek) z przywróceniem poprzedniego stanu
    @PostMapping("/{shopId}/opening-hours")
    public BookShopOpeningHoursUpdateResult updateMondayOpeningHours(
            @RequestParam String db,
            @PathVariable("shopId") String shopId,
            @RequestParam("opensAt") @DateTimeFormat(pattern = "HH:mm:ss") LocalTime opensAt,
            @RequestParam("closesAt") @DateTimeFormat(pattern = "HH:mm:ss") LocalTime closesAt,
            @RequestParam(defaultValue = "true") boolean restoreAfterUpdate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookShopOpeningHoursUpdateService
                .updateMondayOpeningHours(Long.parseLong(shopId), opensAt, closesAt, restoreAfterUpdate);
            case "MSSQL" -> this.mssqlBookShopOpeningHoursUpdateService
                .updateMondayOpeningHours(Long.parseLong(shopId), opensAt, closesAt, restoreAfterUpdate);
            case "CASSANDRA" -> this.cassandraAnalyticsService
                .updateMondayOpeningHours(UUID.fromString(shopId), opensAt, closesAt, restoreAfterUpdate);
            case "SCYLLA" -> this.scyllaAnalyticsService
                .updateMondayOpeningHours(UUID.fromString(shopId), opensAt, closesAt, restoreAfterUpdate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }

    // U5: Zamknięcie wypożyczeń trwających dłużej niż N dni (z przywróceniem poprzedniego stanu)
    @PostMapping("/rentals/close-overdue")
    public BookRentalCloseOverdueResult closeOverdueRentals(
            @RequestParam String db,
            @RequestParam(defaultValue = "30") int daysThreshold,
            @RequestParam(defaultValue = "true") boolean restoreAfterUpdate
    ) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookRentalUpdateService
                    .closeOverdueRentals(daysThreshold, restoreAfterUpdate);
            case "MSSQL" -> this.mssqlBookRentalUpdateService
                    .closeOverdueRentals(daysThreshold, restoreAfterUpdate);
            case "CASSANDRA" -> this.cassandraAnalyticsService
                .closeOverdueRentals(daysThreshold, restoreAfterUpdate);
            case "SCYLLA" -> this.scyllaAnalyticsService
                .closeOverdueRentals(daysThreshold, restoreAfterUpdate);
            default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
        };
    }
                // R5: Ranking najczęściej wypożyczanych książek per sklep (CTE + window function)
                @GetMapping("/{shopId}/book-rental-ranking")
                public List<BookRentalRanking> getBookRentalRankingByShop(
                        @RequestParam String db,
                        @PathVariable("shopId") String shopId) {
                    // Komentarz po polsku: Zwraca ranking najczęściej wypożyczanych książek dla danego sklepu
                    return switch (db) {
                        case "POSTGRESQL" -> this.postgresBookRentalRankingService.getBookRentalRankingByShop(Integer.parseInt(shopId));
                        case "MSSQL" -> this.mssqlBookRentalRankingService.getBookRentalRankingByShop(Integer.parseInt(shopId));
                        case "CASSANDRA" -> this.cassandraAnalyticsService.getBookRentalRankingByShop(UUID.fromString(shopId));
                        case "SCYLLA" -> this.scyllaAnalyticsService.getBookRentalRankingByShop(UUID.fromString(shopId));
                        default -> throw new IllegalArgumentException("Unknown database: " + db);
                    };
                }
            // R4: Obciążenie pracowników (ile wypożyczeń obsłużyli), globalnie ze wszystkich sklepów
            @GetMapping("/employee-rental-counts")
            public List<EmployeeRentalCount> getEmployeeRentalCounts(
                    @RequestParam String db) {
                return switch (db) {
                    case "POSTGRESQL" -> this.postgresBookRentalStatsService.getEmployeeRentalCountsGlobal();
                    case "MSSQL" -> this.mssqlBookRentalStatsService.getEmployeeRentalCountsGlobal();
                    case "CASSANDRA" -> this.cassandraAnalyticsService.getEmployeeRentalCountsGlobal();
                    case "SCYLLA" -> this.scyllaAnalyticsService.getEmployeeRentalCountsGlobal();
                    default -> throw new IllegalArgumentException("Unknown database: " + db);
                };
            }
        // R3: Pobierz rezerwacje dla sklepu
        @GetMapping("/{shopId}/reservations")
        public List<bench.app.model.common.BookReservation> getBookReservations(
                @RequestParam String db,
                @PathVariable("shopId") String shopId) {
            return switch (db) {
                case "POSTGRESQL" -> this.postgresBookReservationService.getBookReservationsByShopId(Long.parseLong(shopId));
                case "MSSQL" -> this.mssqlBookReservationService.getBookReservationsByShopId(Long.parseLong(shopId));
                default -> throw new IllegalArgumentException("Unknown database: " + db);
            };
        }
    // R6: Pobierz wypożyczenia dla sklepu
    @GetMapping("/{shopId}/rentals")
    public List<bench.app.model.common.BookRental> getBookRentals(
            @RequestParam String db,
            @PathVariable("shopId") String shopId) {
        return switch (db) {
            case "POSTGRESQL" -> this.postgresBookRentalService.getBookRentalsByShopId(Long.parseLong(shopId));
            case "MSSQL" -> this.mssqlBookRentalService.getBookRentalsByShopId(Long.parseLong(shopId));
            default -> throw new IllegalArgumentException("Unknown database: " + db);
        };
    }

    // R1: Lista dostępnych książek w danym oddziale/sklepie

    @GetMapping("/{shopId}/books")
    public List<BookListItem> getBooks(
            @RequestParam String db,
            @PathVariable("shopId") String shopId,
            @RequestParam(defaultValue = "false") boolean onlyAvailable) {
        List<Book> books = switch (db) {
            case "POSTGRESQL" -> this.postgresService.getBooks(Long.parseLong(shopId), onlyAvailable);
            case "MSSQL" -> this.mssqlService.getBooks(Long.parseLong(shopId), onlyAvailable);
            case "CASSANDRA" -> this.cassandraService.getBooks(UUID.fromString(shopId), onlyAvailable);
            case "SCYLLA" -> this.scyllaService.getBooks(UUID.fromString(shopId), onlyAvailable);
            default -> throw new IllegalArgumentException("Unknown database: " + db);
        };

        return books.stream()
                .map(book -> new BookListItem(
                        book.author(),
                        book.title(),
                        book.publisher(),
                        book.publishDate(),
                        book.pages(),
                        book.isInReadingRoom()
                ))
                .toList();
    }
}
