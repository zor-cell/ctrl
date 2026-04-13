package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.*;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final IdAustriaOidcService idAustriaOidcService;

    public UserDetailsResponse getUser(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        return userMapper.toUserDetailsResponse(user);
    }

    @Transactional
    public UserDetailsResponse updateUser(User currentUser, UpdateProfileRequest request) {
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        user.setName(request.name());
        user.setEmail(request.email());
        user.setBirthdate(request.birthdate());

        User updatedUser = userRepository.save(user);
        return userMapper.toUserDetailsResponse(updatedUser);
    }

    @Transactional
    public void updatePassword(User currentUser, UpdatePasswordRequest request) {
        User user = userRepository.findById(currentUser.getId()).orElseThrow();

        if(!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepository.save(user);
    }

    @Transactional
    public AuthResponse updateUsername(User currentUser, UpdateUsernameRequest request) {
        String newUsername = request.newUsername().trim();

        if (userRepository.findByUsername(newUsername).isPresent()) {
            throw new ResourceAlreadyExistsException("Username is already taken");
        }

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        user.setUsername(newUsername);
        userRepository.save(user);

        String newToken = jwtService.generateToken(user);
        return new AuthResponse(newToken);
    }

    @Transactional
    public UserDetailsResponse verifyUser(User currentUser, VerifyIdentityRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new InvalidRequestException("Authorization code is missing");
        }

        Jwt jwt = idAustriaOidcService.getTokenFromCode(request.code());

        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        if(firstName != null && lastName != null) {
            currentUser.setName(firstName + " " + lastName);
        }

        String birthdateStr = jwt.getClaimAsString("birthdate");
        if(birthdateStr != null) {
            currentUser.setBirthdate(LocalDate.parse(birthdateStr));
        }

        currentUser.setVerified(true);

        User updatedUser = userRepository.save(currentUser);
        return userMapper.toUserDetailsResponse(updatedUser);
    }
}
