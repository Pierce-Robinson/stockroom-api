package dev.piercerobinson.stockroom.user;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class UserDtos {

	private UserDtos() {
	}

	public record CreateUserRequest(
			@NotBlank @Email @Size(max = 255) String email,
			@NotBlank @Size(min = 8, max = 72, message = "must be between 8 and 72 characters") String password,
			@NotBlank @Size(max = 150) String fullName,
			@NotNull Role role) {
	}

	public record UserResponse(Long id, String email, String fullName, Role role, boolean enabled, Instant createdAt) {

		static UserResponse from(AppUser user) {
			return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
					user.isEnabled(), user.getCreatedAt());
		}

	}

}
