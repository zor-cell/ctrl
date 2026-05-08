package at.ac.tuwien.inso.ctrl.feed.service.impl;

import at.ac.tuwien.inso.ctrl.algorithm.service.RecommendationService;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.entity.*;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

  private final PostRepository postRepository;
  private final PostVoteRepository postVoteRepository;
  private final SourceVoteRepository sourceVoteRepository;
  private final UserRepository userRepository;
  private final FeedMapper feedMapper;
  private final RecommendationService recommendationService;

  @Override
  @Transactional(readOnly = true)
  public List<FeedPostResponse> getFeed(int page, int postsPerPage, String username) {
    User user =
            userRepository
                    .findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    Optional<String> userId = Optional.of(user.getId().toString());

    List<RecommendedPost> recommendedPosts = recommendationService.getRecommendedPosts(user, page, postsPerPage);
    List<String> postIds = recommendedPosts.stream().map(p -> p.getPost().getPostId()).collect(Collectors.toList());

    Map<String, String> postVotes =
            userId
                    .map(
                            id ->
                                    postVoteRepository.findByIdUserIdAndIdPostIdIn(id, postIds).stream()
                                            .collect(
                                                    Collectors.toMap(v -> v.getId().getPostId(), PostVote::getVoteType)))
                    .orElse(Map.of());

    List<String> sourceIds =
            recommendedPosts.stream()
                    .filter(p -> p.getPost().getSource() != null)
                    .map(p -> p.getPost().getSource().getSourceId())
                    .toList();

    Map<String, String> sourceVotes =
            userId
                    .map(
                            id ->
                                    sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(id, sourceIds).stream()
                                            .collect(
                                                    Collectors.toMap(
                                                            v -> v.getId().getSourceId(), SourceVote::getVoteType)))
                    .orElse(Map.of());

    return recommendedPosts.stream()
            .map(post -> feedMapper.toFeedPostResponse(post, postVotes, sourceVotes))
            .toList();
  }

  private String fetchUserId(String username) {
    return userRepository
            .findByUsername(username)
            .map(user -> user.getId().toString())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }

  private Optional<String> validateInputAndResolveUser(
          int page, int postsPerPage, String username) {
    if (page < 0) {
      throw new IllegalArgumentException("Page cannot be negative");
    }
    if (postsPerPage < 1 || postsPerPage > 100) {
      throw new IllegalArgumentException("PostsPerPage must be between 1 and 100");
    }

    return Optional.ofNullable(username).map(this::fetchUserId);
  }
}

