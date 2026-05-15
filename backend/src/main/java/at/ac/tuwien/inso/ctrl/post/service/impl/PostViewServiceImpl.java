package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewBatchRequest;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.post.service.PostViewService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostViewServiceImpl implements PostViewService {

  private final SeenPostRepository seenPostRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void recordViews(PostViewBatchRequest request, String username) {
    User user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

    request
        .views()
        .forEach(v -> seenPostRepository.upsertView(user.getId(), v.postId(), v.durationMs()));
  }
}
