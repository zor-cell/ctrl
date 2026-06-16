package at.ac.tuwien.inso.ctrl.feed.service;

import at.ac.tuwien.inso.ctrl.algorithm.service.RecommendationService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.service.LabelingService;
import at.ac.tuwien.inso.ctrl.post.entity.*;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

  private final PostVoteRepository postVoteRepository;
  private final SourceVoteRepository sourceVoteRepository;
  private final UserRepository userRepository;
  private final FeedMapper feedMapper;
  private final RecommendationService recommendationService;
  private final LabelingService labelingService;
  private final ReportService reportService;

  @Transactional(readOnly = true)
  public List<FeedPostResponse> getFeed(
          int page, int postsPerPage, boolean publicFeed, String username) {
    if (page < 0) {
      throw new InvalidRequestException("Page cannot be negative");
    }
    if (postsPerPage < 1 || postsPerPage > 100) {
      throw new InvalidRequestException("PostsPerPage must be between 1 and 100");
    }

    User user =
            userRepository
                    .findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    Optional<String> userId = Optional.of(user.getId().toString());

    List<RecommendedPost> recommendedPosts =
            recommendationService.getRecommendedPosts(user, page, postsPerPage, publicFeed);
    List<String> postIds =
            recommendedPosts.stream().map(p -> p.getPost().getPostId()).collect(Collectors.toList());

    Map<String, Boolean> postReportedByUser =
            postIds.isEmpty()
                    ? Map.of()
                    : postIds.stream()
                    .collect(
                            Collectors.toMap(
                                    id -> id,
                                    id -> reportService.wasReportedByUser(user, ContentTypeEnum.POST, id)));

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

    Map<String, Boolean> sourceReportedByUser =
            sourceIds.isEmpty()
                    ? Map.of()
                    : sourceIds.stream()
                    .distinct()
                    .collect(
                            Collectors.toMap(
                                    id -> id,
                                    id -> reportService.wasReportedByUser(user, ContentTypeEnum.SOURCE, id)));

    Map<String, String> sourceVotes =
            userId
                    .map(
                            id ->
                                    sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(id, sourceIds).stream()
                                            .collect(
                                                    Collectors.toMap(
                                                            v -> v.getId().getSourceId(), SourceVote::getVoteType)))
                    .orElse(Map.of());

    LabelContext labelCtx = labelingService.buildContext(user);

    return recommendedPosts.stream()
            .map(
                    rp ->
                            feedMapper.toFeedPostResponse(
                                    rp,
                                    postVotes,
                                    postReportedByUser,
                                    sourceVotes,
                                    sourceReportedByUser,
                                    labelingService.computeLabels(rp.getPost(), labelCtx,
                                            postReportedByUser.getOrDefault(rp.getPost().getPostId(), false))))
            .toList();
  }
}