package at.ac.tuwien.inso.ctrl.config.security;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  @Value("${application.security.jwt.secret-key}")
  private String jwtSecret;

  @Value("${application.security.jwt.expiration}")
  private long jwtExpirationMs;

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public Optional<UUID> extractUserId(String token) {
    String userId = extractClaim(token, claims -> claims.get("userId", String.class));
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }

    try {
      return Optional.of(UUID.fromString(userId));
    } catch (IllegalArgumentException ignored) {
      return Optional.empty();
    }
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  public String generateToken(UserDetails userDetails) {
    List<String> roles =
        userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();

    JwtBuilder builder =
        Jwts.builder()
            .claim("roles", roles)
            .subject(userDetails.getUsername())
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs));

    if (userDetails instanceof User user) {
      builder.claim("userId", user.getId().toString());
    }

    return builder
        .signWith(getSignInKey())
        .compact();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    if (isTokenExpired(token)) {
      return false;
    }

    Optional<UUID> tokenUserId = extractUserId(token);
    if (tokenUserId.isPresent() && userDetails instanceof User user) {
      return tokenUserId.get().equals(user.getId());
    }

    final String username = extractUsername(token);
    return username.equals(userDetails.getUsername());
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(getSignInKey()).build().parseSignedClaims(token).getPayload();
  }

  private SecretKey getSignInKey() {
    return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }
}
