package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.UsernameExistsException;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtDecoder keycloakJwtDecoder;
    private final IdAustriaOidcService idAustriaOidcService;

    public UserDetailsResponse getUser(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        return userMapper.toUserDetailsResponse(user);
    }

    @Transactional
    public UserDetailsResponse updateUser(User principal, UpdateProfileRequest request) {
        var user = userRepository.findById(principal.getId()).orElseThrow();

        user.setName(request.name());
        user.setBirthdate(request.birthdate());

        userRepository.save(user);

        return userMapper.toUserDetailsResponse(user);
    }

    @Transactional
    public void updatePassword(User principal, UpdatePasswordRequest request) {
        var user = userRepository.findById(principal.getId()).orElseThrow();

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse updateUsername(User principal, UpdateUsernameRequest request) {
        String newUsername = request.newUsername().trim();

        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new UsernameExistsException("Username is already taken");
        }

        var user = userRepository.findById(principal.getId()).orElseThrow();
        user.setUsername(newUsername);
        userRepository.save(user);

        String newToken = jwtService.generateToken(user);
        return new AuthResponse(newToken);
    }

    @Transactional
    public UserDetailsResponse verifyUser(User currentUser, VerifyIdentityRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Keycloak token is missing");
        }

        try {
            // 1. Exchange the Authorization Code for Tokens
            IdAustriaTokenResponse tokenResponse = idAustriaOidcService.exchangeCodeForToken(request.code());

            if (tokenResponse == null || tokenResponse.idToken() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to retrieve token from ID Austria");
            }

            // 2. Decode and securely validate the id_token
            Jwt jwt = keycloakJwtDecoder.decode(tokenResponse.idToken());

            // 3. Extract ID Austria specific claims
            // The bPK is the only reliable way to uniquely identify the user across sessions
            String bpk = jwt.getClaimAsString("urn:pvpgvat:oidc.bpk");

            // Extract birthdate (format: YYYY-MM-DD)
            String birthdateStr = jwt.getClaimAsString("birthdate");

            LocalDate birthdate = null;
            if (birthdateStr != null) {
                birthdate = LocalDate.parse(birthdateStr);
            }

            currentUser.setVerified(true);
            currentUser.setBirthdate(birthdate);

            User updatedUser = userRepository.save(currentUser);

            return userMapper.toUserDetailsResponse(updatedUser);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid ID token: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Verification failed: " + e.getMessage());
        }
    }
}
