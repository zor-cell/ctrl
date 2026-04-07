package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.auth.dto.AuthResponse;
import at.ac.tuwien.inso.ctrl.exception.InvalidPasswordException;
import at.ac.tuwien.inso.ctrl.exception.UsernameExistsException;
import at.ac.tuwien.inso.ctrl.security.JwtService;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserDetailsResponse getProfile(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        return userMapper.toUserDetailsResponse(user);
    }

    @Transactional
    public UserDetailsResponse updateProfile(User principal, UpdateProfileRequest request) {
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
}
