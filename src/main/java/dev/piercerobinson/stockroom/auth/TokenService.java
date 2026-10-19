package dev.piercerobinson.stockroom.auth;

import java.time.Instant;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Issues signed JWT access tokens for authenticated users. */
@Service
public class TokenService {

	static final String ROLES_CLAIM = "roles";

	private final JwtEncoder encoder;

	private final JwtProperties properties;

	public TokenService(JwtEncoder encoder, JwtProperties properties) {
		this.encoder = encoder;
		this.properties = properties;
	}

	public TokenResponse issue(Authentication authentication) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.ttl());
		List<String> roles = authentication.getAuthorities()
			.stream()
			.map(GrantedAuthority::getAuthority)
			.map(authority -> authority.replaceFirst("^ROLE_", ""))
			.toList();
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(properties.issuer())
			.issuedAt(now)
			.expiresAt(expiresAt)
			.subject(authentication.getName())
			.claim(ROLES_CLAIM, roles)
			.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
		return new TokenResponse(token, "Bearer", properties.ttl().toSeconds(), roles);
	}

	public record TokenResponse(String accessToken, String tokenType, long expiresIn, List<String> roles) {
	}

}
