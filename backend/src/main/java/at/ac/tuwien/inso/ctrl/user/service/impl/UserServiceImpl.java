package at.ac.tuwien.inso.ctrl.user.service.impl;

import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
  private final UserRepository userRepository;

  @Override
  public UserDetailsResponse getCurrentUserDetails(String username) {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    return new UserDetailsResponse(user.getUsername(), user.isVerified());
  }
}
