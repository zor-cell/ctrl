package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserDetailsResponse getProfile(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        return mapToResponse(user);
    }

    @Transactional
    public UserDetailsResponse updateProfile(User principal, UpdateProfileRequest request) {
        var user = userRepository.findById(principal.getId()).orElseThrow();

        user.setName(request.name());
        user.setBirthdate(request.birthdate());

        userRepository.save(user);

        return mapToResponse(user);
    }

    private UserDetailsResponse mapToResponse(User user) {
        return new UserDetailsResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getBirthdate(),
                user.getCreatedAt()
        );
    }
}
