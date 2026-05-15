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

	public record UpdateUserAccountPermissionRequest(String permission, String details) {
	}
}
