package at.ac.tuwien.inso.ctrl.user.mapper;

import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {
  private final FileStorageService fileStorageService;

  public UserDetailsResponse toUserDetailsResponse(User user) {
    return new UserDetailsResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getName(),
        user.getBirthdate(),
        fileStorageService.resolveFileUrl(user.getProfilePicture()),
        user.getCreatedAt(),
        user.isVerified());
  }
}
