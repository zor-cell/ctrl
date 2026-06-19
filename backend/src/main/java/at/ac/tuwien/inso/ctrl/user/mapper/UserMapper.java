package at.ac.tuwien.inso.ctrl.user.mapper;

import at.ac.tuwien.inso.ctrl.user.dto.PublicUserProfileResponse;
import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

  public UserDetailsResponse toUserDetailsResponse(User user) {
    return new UserDetailsResponse(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getName(),
        user.getBio(),
        user.getBirthdate(),
        user.getProfilePicture(),
        user.getCreatedAt(),
        user.isVerified());
  }

  public PublicUserProfileResponse toPublicUserProfileResponse(
      User user, boolean canSeePrivatePosts, boolean wasReportedByUser) {
    return new PublicUserProfileResponse(
        user.getId(),
        user.getUsername(),
        user.isVerified() ? user.getName() : null,
        user.getBio(),
        user.getProfilePicture(),
        user.isVerified(),
        user.isDeleted(),
        canSeePrivatePosts,
        wasReportedByUser);
  }
}
