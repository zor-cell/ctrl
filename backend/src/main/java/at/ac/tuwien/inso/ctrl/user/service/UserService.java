package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserMapper userMapper;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final IdAustriaOidcService idAustriaOidcService;

  public UserDetailsResponse getUser(UUID userId) {
    User user = getUserInternal(userId);
    return userMapper.toUserDetailsResponse(user);
  }

  @Transactional
  public UserDetailsResponse updateUser(User currentUser, UpdateProfileRequest request) {
    User user = getUserInternal(currentUser.getId());

    String newEmail = request.email().trim();
    if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
      throw new ResourceAlreadyExistsException("User", "email", newEmail);
    }

    String newName = request.name().trim();
    if (user.isVerified()) {
      if (!user.getName().equalsIgnoreCase(newName)) {
        throw new InvalidRequestException("Verified users cannot change their name");
      }

      if (!Objects.equals(user.getBirthdate(), request.birthdate())) {
        throw new InvalidRequestException("Verified users cannot change their birthdate");
      }
    }

    user.setName(newName);
    user.setEmail(newEmail);
    user.setBirthdate(request.birthdate());

    User updatedUser = userRepository.save(user);
    return userMapper.toUserDetailsResponse(updatedUser);
  }

  @Transactional
  public void updatePassword(User currentUser, UpdatePasswordRequest request) {
    User user = getUserInternal(currentUser.getId());

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      throw new InvalidPasswordException("Current password is incorrect");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));

    userRepository.save(user);
  }

  @Transactional
  public AuthResponse updateUsername(User currentUser, UpdateUsernameRequest request) {
    String newUsername = request.newUsername().trim();

    if (userRepository.existsByUsername(newUsername)) {
      throw new ResourceAlreadyExistsException("User", "username", newUsername);
    }

    User user = getUserInternal(currentUser.getId());
    user.setUsername(newUsername);
    userRepository.save(user);

    String newToken = jwtService.generateToken(user);
    return new AuthResponse(newToken);
  }

  @Transactional
  public UserDetailsResponse verifyUser(User currentUser, VerifyIdentityRequest request) {
    Jwt jwt;
    try {
      jwt = idAustriaOidcService.getTokenFromCode(request.code());
    } catch (Exception e) {
      throw new CommunicationException("Failed to verify identity with external provider");
    }

    User user = getUserInternal(currentUser.getId());

    String firstName = jwt.getClaimAsString("given_name");
    String lastName = jwt.getClaimAsString("family_name");
    if (firstName != null && lastName != null) {
      user.setName(firstName + " " + lastName);
    }

    String birthdateStr = jwt.getClaimAsString("birthdate");
    if (birthdateStr == null || birthdateStr.isBlank()) {
      throw new InvalidRequestException(
          "Verification failed: No birthdate provided by external provider");
    }

    try {
      user.setBirthdate(LocalDate.parse(birthdateStr));
    } catch (DateTimeParseException e) {
      throw new InvalidRequestException(
          "Verification failed: Invalid birthdate format received from external provider");
    }

    user.setVerified(true);

    User updatedUser = userRepository.save(user);
    return userMapper.toUserDetailsResponse(updatedUser);
  }

  private User getUserInternal(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
