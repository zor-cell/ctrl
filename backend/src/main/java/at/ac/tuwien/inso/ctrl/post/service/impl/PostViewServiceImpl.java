package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.post.dto.request.PostViewBatchRequest;
import at.ac.tuwien.inso.ctrl.post.repository.SeenPostRepository;
import at.ac.tuwien.inso.ctrl.post.service.PostViewService;
import at.ac.tuwien.inso.ctrl.session.entity.SessionHistory;
import at.ac.tuwien.inso.ctrl.session.repository.SessionRepository;
import at.ac.tuwien.inso.ctrl.session.services.SessionService;
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
  private final SessionService sessionService;
  private final SessionRepository sessionRepository;

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

    if (!request.views().isEmpty()) {
      SessionHistory currentSession = sessionService.getOrCreateLastSession(user);

      int newViewCount = currentSession.getPostsViewed() + request.views().size();
      currentSession.setPostsViewed(newViewCount);

      sessionRepository.save(currentSession);
    }
  }
}
