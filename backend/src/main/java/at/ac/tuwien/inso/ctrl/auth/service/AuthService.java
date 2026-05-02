package at.ac.tuwien.inso.ctrl.auth.service;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.auth.dto.LoginRequest;
import at.ac.tuwien.inso.ctrl.auth.dto.RegisterRequest;
import at.ac.tuwien.inso.ctrl.exception.EmailNotVerifiedException;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final AuthenticationManager authenticationManager;

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  public void register(RegisterRequest registerRequest) {
    String username = registerRequest.username().trim();
    if (userRepository.existsByUsername(username)) {
      throw new ResourceAlreadyExistsException("User", "username", username);
    }

    String email = registerRequest.email().trim();
    if (userRepository.existsByEmail(email)) {
      throw new ResourceAlreadyExistsException("User", "email", email);
    }

    var user =
        User.builder()
            .username(username)
            .password(passwordEncoder.encode(registerRequest.password()))
            .name(registerRequest.name().trim())
            .email(email)
            .birthdate(registerRequest.birthdate())
            .emailVerified(!requireEmailVerification)
            .build();

    userRepository.save(user);
  }

  public AuthResponse login(LoginRequest loginRequest) {
    String username = loginRequest.username().trim();
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(username, loginRequest.password()));
    } catch (BadCredentialsException e) {
      throw new InvalidPasswordException("Invalid username or password");
    } catch (DisabledException e) {
      throw new InvalidPasswordException("Account is disabled. Please contact support.");
    } catch (LockedException e) {
      throw new InvalidPasswordException("Account is locked. Please contact support.");
    }

    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    if (requireEmailVerification) {
      if (!user.isEmailVerified()) {
        throw new EmailNotVerifiedException("Email not verified");
      }
    }

    var token = jwtService.generateToken(user);

    return new AuthResponse(token);
  }
}
