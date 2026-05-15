package bench.app.controller;

import bench.app.service.userpermission.UserPermissionCrudOperations;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class UserController {
	private final UserPermissionCrudOperations userPermissionCrudOperations;

	public UserController(UserPermissionCrudOperations userPermissionCrudOperations) {
		this.userPermissionCrudOperations = userPermissionCrudOperations;
	}

	@PostMapping("/sql/user-account-permissions")
	public Map<String, Object> createUserAccountPermission(@RequestBody CreateUserAccountPermissionRequest request) {
		return userPermissionCrudOperations.create(request.id(), request.permission(), request.details());
	}

	@PostMapping("/sql/book-rental-methods")
	public Map<String, Object> createBookRentalMethod(@RequestBody CreateBookRentalMethodRequest request) {
		return userPermissionCrudOperations.createRentalMethod(request.id(), request.method());
	}

	@PostMapping("/sql/bookshops")
	public Map<String, Object> createBookShop(@RequestBody CreateBookShopRequest request) {
		return userPermissionCrudOperations.createBookShop(
				request.id(),
				request.shopName(),
				request.address(),
				request.email(),
				request.managerId()
		);
	}

	@PostMapping("/sql/users")
	public Map<String, Object> createUserRegistration(@RequestBody CreateUserRegistrationRequest request) {
		return userPermissionCrudOperations.createUserRegistration(
				request.id(),
				request.name(),
				request.surname(),
				request.phoneNumber(),
				request.email(),
				request.login(),
				request.passwordHash(),
				request.cardIdNumber(),
				request.activationStatusId(),
				request.permissionId(),
				request.mainBookShopId()
		);
	}

	@GetMapping("/sql/user-account-permissions/{id}")
	public Map<String, Object> getUserAccountPermission(@PathVariable int id) {
		return userPermissionCrudOperations.read(id);
	}

	@PutMapping("/sql/user-account-permissions/{id}")
	public Map<String, Object> updateUserAccountPermission(
			@PathVariable int id,
			@RequestBody UpdateUserAccountPermissionRequest request
	) {
		return userPermissionCrudOperations.update(id, request.permission(), request.details());
	}

	@DeleteMapping("/sql/user-account-permissions/{id}")
	public Map<String, Object> deleteUserAccountPermission(@PathVariable int id) {
		return userPermissionCrudOperations.delete(id);
	}

	public record CreateUserAccountPermissionRequest(Integer id, String permission, String details) {
	}

	public record CreateBookRentalMethodRequest(Integer id, String method) {
	}

	public record CreateBookShopRequest(Integer id, String shopName, String address, String email, Integer managerId) {
	}

	public record CreateUserRegistrationRequest(
			Integer id,
			String name,
			String surname,
			String phoneNumber,
			String email,
			String login,
			String passwordHash,
			String cardIdNumber,
			Integer activationStatusId,
			Integer permissionId,
			Integer mainBookShopId
	) {
	}

	public record UpdateUserAccountPermissionRequest(String permission, String details) {
	}
}
