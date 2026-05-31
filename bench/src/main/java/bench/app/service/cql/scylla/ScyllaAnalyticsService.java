package bench.app.service.cql.scylla;

import bench.app.model.common.ActiveUser;
import bench.app.model.common.BookRentalRanking;
import bench.app.model.common.BookRentalCloseOverdueResult;
import bench.app.model.common.BookShopOpeningHoursUpdateResult;
import bench.app.model.common.EmployeeRentalCount;
import bench.app.model.common.EmployeeShopAssignmentUpdateResult;
import bench.app.model.common.EngagedUser;
import bench.app.model.common.UserActivationBulkUpdateResult;
import bench.app.model.common.UserGroupShopTransferResult;
import bench.app.model.common.UserPermissionCreateResult;
import bench.app.model.common.UserPermissionUpdateResult;
import bench.app.model.common.UserReservationCount;
import bench.app.service.cql.cassandra.CassandraAnalyticsService;
import com.datastax.oss.driver.api.core.CqlSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class ScyllaAnalyticsService {
    private final CassandraAnalyticsService delegate;

    public ScyllaAnalyticsService(@Qualifier("scyllaSession") CqlSession scyllaSession) {
        this.delegate = new CassandraAnalyticsService(scyllaSession);
    }

    public List<ActiveUser> getActiveUsersByShopId(UUID shopId) {
        return this.delegate.getActiveUsersByShopId(shopId);
    }

    public List<UserReservationCount> getTopUsersByReservationCountGlobal() {
        return this.delegate.getTopUsersByReservationCountGlobal();
    }

    public List<EmployeeRentalCount> getEmployeeRentalCountsGlobal() {
        return this.delegate.getEmployeeRentalCountsGlobal();
    }

    public List<BookRentalRanking> getBookRentalRankingByShop(UUID shopId) {
        return this.delegate.getBookRentalRankingByShop(shopId);
    }

    public List<EngagedUser> getEngagedUsersByPeriod(LocalDate fromDate, LocalDate toDate) {
        return this.delegate.getEngagedUsersByPeriod(fromDate, toDate);
    }

    public UserActivationBulkUpdateResult setUsersInactiveIfNoOpenRentalOrReservation(boolean restoreAfterUpdate) {
        return this.delegate.setUsersInactiveIfNoOpenRentalOrReservation(restoreAfterUpdate);
    }

    public UserPermissionUpdateResult updateUserPermissions(UUID userId, long permissionsId, boolean restoreAfterUpdate) {
        return this.delegate.updateUserPermissions(userId, permissionsId, restoreAfterUpdate);
    }

    public UserPermissionCreateResult createPermission(String permission, String details, boolean restoreAfterCreate) {
        return this.delegate.createPermission(permission, details, restoreAfterCreate);
    }

    public BookShopOpeningHoursUpdateResult updateMondayOpeningHours(
            UUID shopId,
            LocalTime opensAtMonday,
            LocalTime closesAtMonday,
            boolean restoreAfterUpdate
    ) {
        return this.delegate.updateMondayOpeningHours(shopId, opensAtMonday, closesAtMonday, restoreAfterUpdate);
    }

    public EmployeeShopAssignmentUpdateResult reassignEmployeeToShop(
            UUID employeeId,
            UUID newShopId,
            boolean restoreAfterUpdate
    ) {
        return this.delegate.reassignEmployeeToShop(employeeId, newShopId, restoreAfterUpdate);
    }

    public BookRentalCloseOverdueResult closeOverdueRentals(int daysThreshold, boolean restoreAfterUpdate) {
        return this.delegate.closeOverdueRentals(daysThreshold, restoreAfterUpdate);
    }

    public UserGroupShopTransferResult transferUserGroup(
            UUID sourceShopId,
            UUID targetShopId,
            int maxUsers,
            boolean restoreAfterUpdate
    ) {
        return this.delegate.transferUserGroup(sourceShopId, targetShopId, maxUsers, restoreAfterUpdate);
    }
}