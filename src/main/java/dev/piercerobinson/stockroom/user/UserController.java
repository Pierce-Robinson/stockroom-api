package dev.piercerobinson.stockroom.user;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.piercerobinson.stockroom.user.UserDtos.CreateUserRequest;
import dev.piercerobinson.stockroom.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping
	@Operation(summary = "List all users")
	public List<UserResponse> list() {
		return userService.list();
	}

	@PostMapping
	@Operation(summary = "Create a user")
	public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
		UserResponse created = userService.create(request);
		return ResponseEntity.created(URI.create("/api/users/" + created.id())).body(created);
	}

	@PostMapping("/{id}/disable")
	@Operation(summary = "Disable a user so they can no longer log in")
	public UserResponse disable(@PathVariable Long id) {
		return userService.setEnabled(id, false);
	}

	@PostMapping("/{id}/enable")
	@Operation(summary = "Re-enable a disabled user")
	public UserResponse enable(@PathVariable Long id) {
		return userService.setEnabled(id, true);
	}

}
