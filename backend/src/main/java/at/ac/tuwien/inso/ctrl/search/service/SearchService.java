package at.ac.tuwien.inso.ctrl.search.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.AlgorithmConfigDto;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.algorithm.service.AlgorithmService;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.search.dto.PostSearchRequest;
import at.ac.tuwien.inso.ctrl.search.dto.UserSearchRequest;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

  private final EmbeddingService embeddingService;
  private final AlgorithmService algorithmService;
  private final PostRepository postRepository;
  private final PostVoteRepository postVoteRepository;
  private final SourceVoteRepository sourceVoteRepository;
  private final UserRepository userRepository;
  private final TopicRepository topicRepository;
  private final FeedMapper feedMapper;
  private final FollowService followService;
  private final FriendRequestRepository friendRequestRepository;
  private final RelationshipMapper relationshipMapper;

  /**
   * Searches for posts based on query embedding similarity with optional topic filtering. The
   * search follows a candidate pool approach: 1. Optional topic filtering (OR logic) 2. Select top
   * N posts by cosine similarity to query embedding 3. Apply algorithmic scoring to rank the
   * candidate pool
   */
  @Transactional(readOnly = true)
  public List<FeedPostResponse> searchPosts(PostSearchRequest request, User user) {
    // Validate that we have at least a query or topics
    boolean hasQuery =
        request.query() != null && !request.query().isBlank() && request.query().length() >= 3;
    boolean hasTopics = request.topicIds() != null && !request.topicIds().isEmpty();

    if (!hasQuery && !hasTopics) {
      throw new InvalidRequestException(
          "Search requires either a query (3+ characters) or at least one topic");
    }

    // Embed the search query if present
    float[] queryEmbedding = null;
    if (hasQuery) {
      queryEmbedding = embeddingService.embedText(request.query().trim());
    }

    // Get user's algorithm configuration for personalization
    AlgorithmConfigDto config = algorithmService.getConfig(user);

    // Calculate preference vectors (same logic as RecommendationService)
    List<Float> interactionVectorList =
        userRepository.calculateUserInteractionPreferenceVector(user.getId());
    float[] interactionPreference = vectorFromVectorList(interactionVectorList);

    List<Float> topicVectorList =
        topicRepository.calculateTopicPreferenceVector(
            config.topics() != null ? config.topics() : Collections.emptyList());
    float[] topicPreference = vectorFromVectorList(topicVectorList);

    // Convert topicIds to array for the query
    String[] topicIdsArray = hasTopics ? request.topicIds().toArray(new String[0]) : new String[0];

    // Call the repository to get scored posts
    List<PostScoreProjection> scoredProjections =
        postRepository.searchPostsByEmbedding(
            user.getId(),
            queryEmbedding,
            topicIdsArray,
            config.weightInteraction() / 100.0,
            interactionPreference,
            config.weightTopic() / 100.0,
            topicPreference,
            config.weightRecency() / 100.0,
            config.weightTrending() / 100.0,
            config.weightSourceQuality() / 100.0,
            config.weightExploration() / 100.0,
            request.postsPerPage(),
            request.page() * request.postsPerPage());

    if (scoredProjections.isEmpty()) {
      return Collections.emptyList();
    }

    // Fetch full posts with associations
    List<String> postIds = scoredProjections.stream().map(PostScoreProjection::getPostId).toList();
    List<Post> fullPosts = postRepository.findAllWithAssociationsByIds(postIds);
    Map<String, Post> postMap =
        fullPosts.stream().collect(Collectors.toMap(Post::getPostId, post -> post));

    // Get user votes for posts
    String userIdStr = user.getId().toString();
    Map<String, String> postVotes =
        postVoteRepository.findByIdUserIdAndIdPostIdIn(userIdStr, postIds).stream()
            .collect(Collectors.toMap(v -> v.getId().getPostId(), v -> v.getVoteType()));

    // Get source votes
    List<String> sourceIds =
        scoredProjections.stream()
            .map(score -> postMap.get(score.getPostId()))
            .filter(post -> post != null && post.getSource() != null)
            .map(post -> post.getSource().getSourceId())
            .toList();

    Map<String, String> sourceVotes =
        sourceVoteRepository.findByIdUserIdAndIdSourceIdIn(userIdStr, sourceIds).stream()
            .collect(Collectors.toMap(v -> v.getId().getSourceId(), v -> v.getVoteType()));

    // Map to FeedPostResponse
    return scoredProjections.stream()
        .map(
            score -> {
              Post post = postMap.get(score.getPostId());
              if (post == null) {
                return null;
              }
              RecommendedPost recommendedPost =
                  new RecommendedPost(
                      post,
                      new at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation(
                          score.getScoreInteraction(),
                          score.getScoreTopic(),
                          score.getScoreRecency(),
                          score.getScoreTrending(),
                          score.getScoreSource(),
                          score.getScoreExploration(),
                          score.getTotalScore()));
              return feedMapper.toFeedPostResponse(recommendedPost, postVotes, sourceVotes);
            })
        .filter(response -> response != null)
        .toList();
  }

  /** Searches for users based on username matching with optional relationship filters. */
  @Transactional(readOnly = true)
  public List<UserSearchResponse> searchUsers(UserSearchRequest request, User currentUser) {
    String searchTerm = request.query() != null ? request.query().trim().toLowerCase() : "";

    // Get relationship sets
    Set<UUID> followedIds = followService.getFollowedIds(currentUser);
    Set<UUID> friendIds = followService.getFriendIds(currentUser);
    Set<UUID> pendingRequestSentIds = friendRequestRepository.findReceiverIdsBySender(currentUser);
    Set<UUID> pendingRequestIncomingIds =
        friendRequestRepository.findSenderIdsByReceiver(currentUser);

    // Find users matching the search term
    List<User> allUsers = userRepository.findByUsernameContainingIgnoreCase(searchTerm);

    return allUsers.stream()
        .filter(user -> !user.getId().equals(currentUser.getId()))
        .filter(user -> !request.excludeFriends() || !friendIds.contains(user.getId()))
        .filter(user -> !request.excludeFollowed() || !followedIds.contains(user.getId()))
        .filter(user -> !request.verifiedOnly() || user.isVerified())
        .map(
            user -> {
              UserSearchResponse.RelationshipStatus status =
                  relationshipMapper.determineRelationshipStatus(
                      user,
                      followedIds,
                      friendIds,
                      pendingRequestSentIds,
                      pendingRequestIncomingIds);
              String displayName = relationshipMapper.displayName(user, status);
              return relationshipMapper.toUserSearchResponse(user, status, displayName);
            })
        .toList();
  }

  private float[] vectorFromVectorList(List<Float> vectorList) {
    if (vectorList == null || vectorList.isEmpty()) {
      return null;
    }

    boolean hasData = false;
    float[] vector = new float[vectorList.size()];
    for (int i = 0; i < vectorList.size(); i++) {
      float val = vectorList.get(i) != null ? vectorList.get(i) : 0f;
      vector[i] = val;
      if (val != 0f) {
        hasData = true;
      }
    }

    return hasData ? vector : null;
  }
}
