package at.ac.tuwien.inso.ctrl.search.service;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostPreviewResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.search.dto.PostSearchRequest;
import at.ac.tuwien.inso.ctrl.search.dto.UserSearchRequest;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

  private final EmbeddingService embeddingService;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final PostMapper postMapper;
  private final FollowService followService;
  private final FriendRequestRepository friendRequestRepository;
  private final RelationshipMapper relationshipMapper;

  @Transactional(readOnly = true)
  public List<PostPreviewResponse> searchPosts(PostSearchRequest request, User user) {
    boolean hasQuery =
        request.query() != null && !request.query().isBlank() && request.query().length() >= 3;
    boolean hasTopics = request.topicIds() != null && !request.topicIds().isEmpty();

    float[] queryEmbedding = null;
    if (hasQuery) {
      queryEmbedding = embeddingService.embedText(request.query().trim());
    }

    String[] queryWords = hasQuery ? request.query().trim().split(" ") : new String[0];

    String[] topicIdsArray = hasTopics ? request.topicIds().toArray(new String[0]) : new String[0];

    List<Post> posts;

    if (!hasQuery && hasTopics) {
      posts =
          postRepository.searchPostsByTopics(
              user.getId(),
              topicIdsArray,
              request.postsPerPage(),
              request.page() * request.postsPerPage());
    } else if (hasQuery && !hasTopics) {
      posts =
          postRepository.searchPostsBySimilarity(
              user.getId(),
              queryWords,
              queryEmbedding,
              request.postsPerPage(),
              request.page() * request.postsPerPage());
    } else if (hasQuery && hasTopics) {
      posts =
          postRepository.searchPostsBySimilarityAndTopics(
              user.getId(),
              queryWords,
              queryEmbedding,
              topicIdsArray,
              request.postsPerPage(),
              request.page() * request.postsPerPage());
    } else {
      throw new InvalidRequestException(
          "Search requires either a query (3+ characters) or at least one topic");
    }

    if (posts.isEmpty()) {
      return Collections.emptyList();
    }

    return posts.stream().map(postMapper::toPostPreviewResponse).toList();
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
              List<RelationshipStatusItem> stati =
                  relationshipMapper.determineRelationshipStatuses(
                      user,
                      followedIds,
                      friendIds,
                      pendingRequestSentIds,
                      pendingRequestIncomingIds);
              String displayName = relationshipMapper.displayName(user, stati);
              return relationshipMapper.toUserSearchResponse(user, stati, displayName);
            })
        .toList();
  }
}
