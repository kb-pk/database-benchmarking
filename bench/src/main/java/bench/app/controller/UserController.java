package bench.app.controller;

import bench.app.model.common.ActiveUser;
import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserActivationBulkUpdateResult;
import bench.app.model.common.UserInactiveSegmentDeleteResult;
import bench.app.model.common.UserPermissionCreateResult;
import bench.app.model.common.UserRegistrationCreateResult;
import bench.app.model.common.UserPermissionUpdateResult;
import bench.app.model.common.UserGroupShopTransferResult;
import bench.app.model.common.UserReservationRentalCount;
import bench.app.service.cql.cassandra.CassandraAnalyticsService;
import bench.app.service.cql.cassandra.CassandraInactiveUserSegmentDeleteService;
import bench.app.service.cql.cassandra.CassandraUserRegistrationCreateService;
import bench.app.service.cql.scylla.ScyllaAnalyticsService;
import bench.app.service.cql.scylla.ScyllaInactiveUserSegmentDeleteService;
import bench.app.service.cql.scylla.ScyllaUserRegistrationCreateService;
import bench.app.service.sql.MssqlUserReadService;
import bench.app.service.sql.MssqlUserAccountUpdateService;
import bench.app.service.sql.MssqlUserAccountPermissionCreateService;
import bench.app.service.sql.MssqlUserRegistrationCreateService;
import bench.app.service.sql.MssqlUserActivationUpdateService;
import bench.app.service.sql.MssqlUserGroupTransferService;
import bench.app.service.sql.MssqlInactiveUserSegmentDeleteService;
import bench.app.service.sql.PostgresUserAccountPermissionCreateService;
import bench.app.service.sql.PostgresUserRegistrationCreateService;
import bench.app.service.sql.PostgresUserAccountUpdateService;
import bench.app.service.sql.PostgresUserActivationUpdateService;
import bench.app.service.sql.PostgresUserGroupTransferService;
import bench.app.service.sql.PostgresInactiveUserSegmentDeleteService;
import bench.app.service.sql.PostgresUserReadService;
import bench.app.service.sql.PostgresBookReservationStatsService;
import bench.app.service.sql.MssqlBookReservationStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/user")
public class UserController {
	private final PostgresUserReadService postgresUserReadService;
	private final MssqlUserReadService mssqlUserReadService;
	private final PostgresBookReservationStatsService postgresBookReservationStatsService;
	private final MssqlBookReservationStatsService mssqlBookReservationStatsService;
	private final PostgresUserAccountUpdateService postgresUserAccountUpdateService;
	private final MssqlUserAccountUpdateService mssqlUserAccountUpdateService;
	private final PostgresUserAccountPermissionCreateService postgresUserAccountPermissionCreateService;
	private final MssqlUserAccountPermissionCreateService mssqlUserAccountPermissionCreateService;
	private final PostgresUserRegistrationCreateService postgresUserRegistrationCreateService;
	private final MssqlUserRegistrationCreateService mssqlUserRegistrationCreateService;
	private final CassandraUserRegistrationCreateService cassandraUserRegistrationCreateService;
	private final ScyllaUserRegistrationCreateService scyllaUserRegistrationCreateService;
	private final PostgresUserActivationUpdateService postgresUserActivationUpdateService;
	private final MssqlUserActivationUpdateService mssqlUserActivationUpdateService;
	private final PostgresUserGroupTransferService postgresUserGroupTransferService;
	private final MssqlUserGroupTransferService mssqlUserGroupTransferService;
	private final PostgresInactiveUserSegmentDeleteService postgresInactiveUserSegmentDeleteService;
	private final MssqlInactiveUserSegmentDeleteService mssqlInactiveUserSegmentDeleteService;
	private final CassandraInactiveUserSegmentDeleteService cassandraInactiveUserSegmentDeleteService;
	private final ScyllaInactiveUserSegmentDeleteService scyllaInactiveUserSegmentDeleteService;
	private final CassandraAnalyticsService cassandraAnalyticsService;
	private final ScyllaAnalyticsService scyllaAnalyticsService;

	public UserController(PostgresUserReadService postgresUserReadService, MssqlUserReadService mssqlUserReadService,
						 PostgresBookReservationStatsService postgresBookReservationStatsService,
						 MssqlBookReservationStatsService mssqlBookReservationStatsService,
						 PostgresUserAccountUpdateService postgresUserAccountUpdateService,
						 MssqlUserAccountUpdateService mssqlUserAccountUpdateService,
						 PostgresUserAccountPermissionCreateService postgresUserAccountPermissionCreateService,
						 MssqlUserAccountPermissionCreateService mssqlUserAccountPermissionCreateService,
						 PostgresUserRegistrationCreateService postgresUserRegistrationCreateService,
						 MssqlUserRegistrationCreateService mssqlUserRegistrationCreateService,
						 CassandraUserRegistrationCreateService cassandraUserRegistrationCreateService,
						 ScyllaUserRegistrationCreateService scyllaUserRegistrationCreateService,
						 PostgresUserActivationUpdateService postgresUserActivationUpdateService,
						 MssqlUserActivationUpdateService mssqlUserActivationUpdateService,
						 PostgresUserGroupTransferService postgresUserGroupTransferService,
						 MssqlUserGroupTransferService mssqlUserGroupTransferService,
						 PostgresInactiveUserSegmentDeleteService postgresInactiveUserSegmentDeleteService,
						 MssqlInactiveUserSegmentDeleteService mssqlInactiveUserSegmentDeleteService,
						 CassandraInactiveUserSegmentDeleteService cassandraInactiveUserSegmentDeleteService,
						 ScyllaInactiveUserSegmentDeleteService scyllaInactiveUserSegmentDeleteService,
						 CassandraAnalyticsService cassandraAnalyticsService,
						 ScyllaAnalyticsService scyllaAnalyticsService) {
		this.postgresUserReadService = postgresUserReadService;
		this.mssqlUserReadService = mssqlUserReadService;
		this.postgresBookReservationStatsService = postgresBookReservationStatsService;
		this.mssqlBookReservationStatsService = mssqlBookReservationStatsService;
		this.postgresUserAccountUpdateService = postgresUserAccountUpdateService;
		this.mssqlUserAccountUpdateService = mssqlUserAccountUpdateService;
		this.postgresUserAccountPermissionCreateService = postgresUserAccountPermissionCreateService;
		this.mssqlUserAccountPermissionCreateService = mssqlUserAccountPermissionCreateService;
		this.postgresUserRegistrationCreateService = postgresUserRegistrationCreateService;
		this.mssqlUserRegistrationCreateService = mssqlUserRegistrationCreateService;
		this.cassandraUserRegistrationCreateService = cassandraUserRegistrationCreateService;
		this.scyllaUserRegistrationCreateService = scyllaUserRegistrationCreateService;
		this.postgresUserActivationUpdateService = postgresUserActivationUpdateService;
		this.mssqlUserActivationUpdateService = mssqlUserActivationUpdateService;
		this.postgresUserGroupTransferService = postgresUserGroupTransferService;
		this.mssqlUserGroupTransferService = mssqlUserGroupTransferService;
		this.postgresInactiveUserSegmentDeleteService = postgresInactiveUserSegmentDeleteService;
		this.mssqlInactiveUserSegmentDeleteService = mssqlInactiveUserSegmentDeleteService;
		this.cassandraInactiveUserSegmentDeleteService = cassandraInactiveUserSegmentDeleteService;
		this.scyllaInactiveUserSegmentDeleteService = scyllaInactiveUserSegmentDeleteService;
		this.cassandraAnalyticsService = cassandraAnalyticsService;
		this.scyllaAnalyticsService = scyllaAnalyticsService;
	}

	// C3: Rejestracja użytkownika (użytkownik + karta + konto) z opcjonalnym przywróceniem stanu
	@PostMapping("/registration/create")
	public UserRegistrationCreateResult createUserRegistration(
			@RequestParam String db,
			@RequestParam String name,
			@RequestParam String surname,
			@RequestParam(required = false) String phoneNumber,
			@RequestParam String email,
			@RequestParam String login,
			@RequestParam String passwordHash,
			@RequestParam(defaultValue = "true") boolean restoreAfterCreate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserRegistrationCreateService
					.createUserRegistration(name, surname, phoneNumber, email, login, passwordHash, restoreAfterCreate);
			case "MSSQL" -> this.mssqlUserRegistrationCreateService
					.createUserRegistration(name, surname, phoneNumber, email, login, passwordHash, restoreAfterCreate);
			case "CASSANDRA" -> this.cassandraUserRegistrationCreateService
					.createUserRegistration(name, surname, phoneNumber, email, login, passwordHash, restoreAfterCreate);
			case "SCYLLA" -> this.scyllaUserRegistrationCreateService
					.createUserRegistration(name, surname, phoneNumber, email, login, passwordHash, restoreAfterCreate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// C1: Dodanie nowego uprawnienia konta (z opcjonalnym przywróceniem stanu)
	@PostMapping("/account-permissions/create")
	public UserPermissionCreateResult createUserAccountPermission(
			@RequestParam String db,
			@RequestParam String permission,
			@RequestParam(required = false) String details,
			@RequestParam(defaultValue = "true") boolean restoreAfterCreate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserAccountPermissionCreateService
					.createPermission(permission, details, restoreAfterCreate);
			case "MSSQL" -> this.mssqlUserAccountPermissionCreateService
					.createPermission(permission, details, restoreAfterCreate);
			case "CASSANDRA" -> this.cassandraAnalyticsService
					.createPermission(permission, details, restoreAfterCreate);
			case "SCYLLA" -> this.scyllaAnalyticsService
					.createPermission(permission, details, restoreAfterCreate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// U1: Aktualizacja uprawnień konta użytkownika (po operacji przywracamy poprzedni stan)
	@PostMapping("/account-permissions")
	public UserPermissionUpdateResult updateUserAccountPermissions(
			@RequestParam String db,
			@RequestParam String userId,
			@RequestParam long permissionsId,
			@RequestParam(defaultValue = "true") boolean restoreAfterUpdate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserAccountUpdateService
					.updateUserPermissions(Long.parseLong(userId), permissionsId, restoreAfterUpdate);
			case "MSSQL" -> this.mssqlUserAccountUpdateService
					.updateUserPermissions(Long.parseLong(userId), permissionsId, restoreAfterUpdate);
			case "CASSANDRA" -> this.cassandraAnalyticsService
					.updateUserPermissions(UUID.fromString(userId), permissionsId, restoreAfterUpdate);
			case "SCYLLA" -> this.scyllaAnalyticsService
					.updateUserPermissions(UUID.fromString(userId), permissionsId, restoreAfterUpdate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// U3: Zmiana statusu na INACTIVE dla wszystkich aktywnych użytkowników
	// bez aktywnego wypożyczenia i bez rezerwacji
	@PostMapping("/activation-status/inactive-if-no-open-items")
	public UserActivationBulkUpdateResult updateUserActivationStatusToInactive(
			@RequestParam String db,
			@RequestParam(required = false) Long userId,
			@RequestParam(defaultValue = "true") boolean restoreAfterUpdate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserActivationUpdateService
					.setUsersInactiveIfNoOpenRentalOrReservation(restoreAfterUpdate);
			case "MSSQL" -> this.mssqlUserActivationUpdateService
					.setUsersInactiveIfNoOpenRentalOrReservation(restoreAfterUpdate);
			case "CASSANDRA" -> this.cassandraAnalyticsService
					.setUsersInactiveIfNoOpenRentalOrReservation(restoreAfterUpdate);
			case "SCYLLA" -> this.scyllaAnalyticsService
					.setUsersInactiveIfNoOpenRentalOrReservation(restoreAfterUpdate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// U6: Przeniesienie grupy czytelników z jednego sklepu do innego
	@PostMapping("/transfer-group-to-shop")
	public UserGroupShopTransferResult transferUserGroupToAnotherShop(
			@RequestParam String db,
			@RequestParam String sourceShopId,
			@RequestParam String targetShopId,
			@RequestParam(defaultValue = "50") int maxUsers,
			@RequestParam(defaultValue = "true") boolean restoreAfterUpdate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserGroupTransferService
					.transferUserGroup(Long.parseLong(sourceShopId), Long.parseLong(targetShopId), maxUsers, restoreAfterUpdate);
			case "MSSQL" -> this.mssqlUserGroupTransferService
					.transferUserGroup(Long.parseLong(sourceShopId), Long.parseLong(targetShopId), maxUsers, restoreAfterUpdate);
			case "CASSANDRA" -> this.cassandraAnalyticsService
					.transferUserGroup(UUID.fromString(sourceShopId), UUID.fromString(targetShopId), maxUsers, restoreAfterUpdate);
			case "SCYLLA" -> this.scyllaAnalyticsService
					.transferUserGroup(UUID.fromString(sourceShopId), UUID.fromString(targetShopId), maxUsers, restoreAfterUpdate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// D4: Segmentowe usuwanie nieaktywnych użytkowników bez aktywności co najmniej N miesięcy
	@PostMapping("/inactive-segment-delete")
	public UserInactiveSegmentDeleteResult deleteInactiveUsersInSegment(
			@RequestParam String db,
			@RequestParam(defaultValue = "3") int monthsThreshold,
			@RequestParam(defaultValue = "50") int segmentSize,
			@RequestParam(defaultValue = "true") boolean restoreAfterDelete
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresInactiveUserSegmentDeleteService
					.deleteInactiveUsersWithoutRecentActivity(monthsThreshold, segmentSize, restoreAfterDelete);
			case "MSSQL" -> this.mssqlInactiveUserSegmentDeleteService
					.deleteInactiveUsersWithoutRecentActivity(monthsThreshold, segmentSize, restoreAfterDelete);
			case "CASSANDRA" -> this.cassandraInactiveUserSegmentDeleteService
					.deleteInactiveUsersWithoutRecentActivity(monthsThreshold, segmentSize, restoreAfterDelete);
			case "SCYLLA" -> this.scyllaInactiveUserSegmentDeleteService
					.deleteInactiveUsersWithoutRecentActivity(monthsThreshold, segmentSize, restoreAfterDelete);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// R3: Lista użytkowników z liczbą rezerwacji i wypożyczeń (globalnie)
	@GetMapping({"/top-by-reservations", "/activity-counts"})
	public List<UserReservationRentalCount> getUsersActivityCountsGlobal(
			@RequestParam String db
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresBookReservationStatsService.getUsersActivityCountsGlobal();
			case "MSSQL" -> this.mssqlBookReservationStatsService.getUsersActivityCountsGlobal();
			case "CASSANDRA" -> this.cassandraAnalyticsService.getUsersActivityCountsGlobal();
			case "SCYLLA" -> this.scyllaAnalyticsService.getUsersActivityCountsGlobal();
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// R2: Aktywni użytkownicy przypisani do sklepu
	@GetMapping("/active-by-shop/{shopId}")
	public List<ActiveUser> getActiveUsersByShop(
		@RequestParam String db,
		@PathVariable("shopId") String shopId
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserReadService.getActiveUsersByShopId(Long.parseLong(shopId));
			case "MSSQL" -> this.mssqlUserReadService.getActiveUsersByShopId(Long.parseLong(shopId));
			case "CASSANDRA" -> this.cassandraAnalyticsService.getActiveUsersByShopId(UUID.fromString(shopId));
			case "SCYLLA" -> this.scyllaAnalyticsService.getActiveUsersByShopId(UUID.fromString(shopId));
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}

	// R6: Użytkownicy "zaangażowani" (mieli i rezerwacje, i wypożyczenia) w zadanym okresie
	@GetMapping("/engaged")
	public List<EngagedUser> getEngagedUsersByPeriod(
			@RequestParam String db,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
			@RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
	) {
		return switch (db) {
			case "POSTGRESQL" -> this.postgresUserReadService.getEngagedUsersByPeriod(fromDate, toDate);
			case "MSSQL" -> this.mssqlUserReadService.getEngagedUsersByPeriod(fromDate, toDate);
			case "CASSANDRA" -> this.cassandraAnalyticsService.getEngagedUsersByPeriod(fromDate, toDate);
			case "SCYLLA" -> this.scyllaAnalyticsService.getEngagedUsersByPeriod(fromDate, toDate);
			default -> throw new IllegalArgumentException("Unsupported db for this endpoint: " + db);
		};
	}
}
