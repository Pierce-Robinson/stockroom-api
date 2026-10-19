package dev.piercerobinson.stockroom.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.piercerobinson.stockroom.common.BusinessRuleException;
import dev.piercerobinson.stockroom.common.NotFoundException;
import dev.piercerobinson.stockroom.user.UserDtos.CreateUserRequest;
import dev.piercerobinson.stockroom.user.UserDtos.UserResponse;

@Service
public class UserService {

	private final UserRepository users;

	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository users, PasswordEncoder passwordEncoder) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String email = request.email().trim().toLowerCase();
		if (users.existsByEmailIgnoreCase(email)) {
			throw new BusinessRuleException("EMAIL_TAKEN", "A user with email %s already exists".formatted(email));
		}
		AppUser user = new AppUser(email, passwordEncoder.encode(request.password()), request.fullName().trim(),
				request.role());
		return UserResponse.from(users.save(user));
	}

	@Transactional(readOnly = true)
	public List<UserResponse> list() {
		return users.findAll().stream().map(UserResponse::from).toList();
	}

	@Transactional
	public UserResponse setEnabled(Long id, boolean enabled) {
		AppUser user = users.findById(id).orElseThrow(() -> new NotFoundException("User", id));
		user.setEnabled(enabled);
		return UserResponse.from(user);
	}

}
