package at.ac.tuwien.inso.ctrl.user.service;

import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }

  public UserDetails loadUserById(@NonNull UUID userId) throws UsernameNotFoundException {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
  }
}
