package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.exception.ResourceAlreadyExistsException;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.BlockedResponse;
import at.ac.tuwien.inso.ctrl.user.dto.FriendRequestResponse;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelation;
import at.ac.tuwien.inso.ctrl.user.entity.BlockedRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.BlockedRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import at.ac.tuwien.inso.ctrl.user.service.FollowService;
import at.ac.tuwien.inso.ctrl.user.service.FriendshipService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.server.ResponseStatusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BlockServiceUnitTest {

  @Mock private FollowService followService;

  @Mock private UserRepository userRepository;

  @Mock private FriendshipService friendshipService;

  @Mock private PostRepository postRepository;

  @Mock private BlockedRelationRepository blockedRelationRepository;

  @Mock private RelationshipMapper relationshipMapper;

  @Mock private CommentRepository commentRepository;

  @Mock private PostVoteRepository postVoteRepository;

  @Mock private SourceVoteRepository sourceVoteRepository;

  @InjectMocks private BlockService blockService;

  private User currentUser;
  private User blockedUser;

  @BeforeEach
  void setUp() {
	currentUser = createUser("currentUser");
	blockedUser = createUser("blockedUser");
  }

  @Test
  public void inBlockedRelationship_withNoBlockedRelationship_shouldReturnFalse() {
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(any(), any())).thenReturn(false);

    assertFalse(blockService.inBlockedRelationship(currentUser, blockedUser));

    verify(blockedRelationRepository).existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser);
    verify(blockedRelationRepository).existsBlockedRelationByUserAndBlockedUser(blockedUser, currentUser);

  }

  @Test
  public void inBlockedRelationship_withABlockedB_shouldReturnTrue() {
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(true);
    assertTrue(blockService.inBlockedRelationship(currentUser, blockedUser));

    verify(blockedRelationRepository).existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser);
  }

  @Test
  public void inBlockedRelationship_withBBlockedA_shouldReturnTrue() {
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(blockedUser, currentUser)).thenReturn(true);

    assertTrue(blockService.inBlockedRelationship(currentUser, blockedUser));

    verify(blockedRelationRepository).existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser);
    verify(blockedRelationRepository).existsBlockedRelationByUserAndBlockedUser(blockedUser, currentUser);
  }
  
  @Test
  public void unblockUser_withValidUserAndValidUsername_shouldRemoveBlockingRelation() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    BlockedRelationId expectedRelationId =
        new BlockedRelationId(currentUser.getId(), blockedUser.getId());
    when(blockedRelationRepository.existsById(expectedRelationId))
        .thenReturn(true);

    blockService.unblockUser(currentUser, blockedUser.getUsername());

    verify(userRepository).findByUsername(blockedUser.getUsername());
    verify(blockedRelationRepository).existsById(expectedRelationId);
    verify(blockedRelationRepository).deleteById(expectedRelationId);
  }

  @Test
  public void unblockUser_withInvalidUserAndValidUsername_shouldRemoveBlockingRelation() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    BlockedRelationId expectedRelationId =
        new BlockedRelationId(currentUser.getId(), blockedUser.getId());
    when(blockedRelationRepository.existsById(expectedRelationId))
        .thenReturn(false);

    assertThrows(EntityNotFoundException.class, () -> blockService.unblockUser(currentUser, blockedUser.getUsername()));

    verify(userRepository).findByUsername(blockedUser.getUsername());
  }

  @Test
  public void unblockUser_withValidUserAndInvalidUsername_shouldThrowException() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> blockService.unblockUser(currentUser, blockedUser.getUsername()));

    verify(userRepository).findByUsername(blockedUser.getUsername());
  }
  
  @Test
  public void blockUser_withValidUserAndUsernameInNoRelationship_shouldBlockUser() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser))
        .thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(userRepository).findByUsername(blockedUser.getUsername());
    verify(blockedRelationRepository)
        .existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser);
    ArgumentCaptor<BlockedRelation> relationCaptor = ArgumentCaptor.forClass(BlockedRelation.class);
    verify(blockedRelationRepository).save(relationCaptor.capture());

    BlockedRelation savedRelation = relationCaptor.getValue();
    assertNotNull(savedRelation);
    assertEquals(currentUser, savedRelation.getUser());
    assertEquals(blockedUser, savedRelation.getBlockedUser());
    assertEquals(new BlockedRelationId(currentUser.getId(), blockedUser.getId()), savedRelation.getId());

    verify(followService, never()).unfollow(any(), any());
    verify(friendshipService, never()).unfriend(any(), any());
    verify(friendshipService, never()).cancelSentRequest(any(), any());
  }

  @Test
  public void blockUser_withValidUserAndInvalidUsername_shouldThrowException() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.empty());

    assertThrows(EntityNotFoundException.class, () -> blockService.blockUser(currentUser, blockedUser.getUsername()));

    verify(userRepository).findByUsername(blockedUser.getUsername());
  }

  @Test
  public void blockUser_withValidUserAndUsernameAlreadyBlocked_shouldThrowException() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser))
        .thenReturn(true);

    assertThrows(ResourceAlreadyExistsException.class, () -> blockService.blockUser(currentUser, blockedUser.getUsername()));

    verify(userRepository).findByUsername(blockedUser.getUsername());
    verify(blockedRelationRepository)
        .existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser);

  }


  @Test
  public void blockUser_whenCurrentUserFollowsTarget_shouldUnfollowTarget() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(true);   // <-- branch hit
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(followService).unfollow(currentUser, blockedUser.getUsername());
  }

  @Test
  public void blockUser_whenTargetFollowsCurrentUser_shouldUnfollowCurrentUser() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(true);   // <-- branch hit
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(followService).unfollow(blockedUser, currentUser.getUsername());
  }

  @Test
  public void blockUser_whenUsersAreFriends_shouldUnfriend() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(true);    // <-- branch hit
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(friendshipService).unfriend(currentUser, blockedUser.getUsername());
  }

  @Test
  public void blockUser_whenBlockedUserSentIncomingRequest_shouldCancelIt() {
    FriendRequestResponse incomingRequest = new FriendRequestResponse(
        blockedUser.getUsername(), null, true, null);

    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of(incomingRequest)); // <-- branch hit
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(friendshipService).cancelSentRequest(blockedUser, currentUser.getUsername());
  }

  @Test
  public void blockUser_whenCurrentUserSentRequestToTarget_shouldCancelIt() {
    FriendRequestResponse sentRequest = new FriendRequestResponse(
        blockedUser.getUsername(), null, false, null);

    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of(sentRequest));         // <-- branch hit
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(friendshipService).cancelSentRequest(currentUser, blockedUser.getUsername());
  }

  @Test
  public void removeBlockedUserContent_withPostNoCommentsNoVoteNoSource_shouldNotDeleteAnything() {
    Post post = createPost("post1", null);
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of());
    when(postVoteRepository.findById(new PostVoteId(post.getPostId(), blockedUser.getId().toString())))
        .thenReturn(Optional.empty());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(commentRepository, never()).delete(any());
    verify(postVoteRepository, never()).delete(any());
    verify(sourceVoteRepository, never()).findById(any());
  }

  @Test
  public void removeBlockedUserContent_withCommentFound_shouldDeleteComment() {
    Post post = createPost("post1", null);
    Comment comment = createComment("c1");
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of(comment));
    when(commentRepository.findById("c1")).thenReturn(Optional.of(comment));
    when(postVoteRepository.findById(any())).thenReturn(Optional.empty());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(commentRepository).delete(comment);
  }

  @Test
  public void removeBlockedUserContent_withCommentNotFoundById_shouldThrowResponseStatusException() {
    Post post = createPost("post1", null);
    Comment comment = createComment("c1");
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of(comment));
    when(commentRepository.findById("c1")).thenReturn(Optional.empty());

    assertThrows(ResponseStatusException.class,
        () -> blockService.blockUser(currentUser, blockedUser.getUsername()));
  }

  @Test
  public void removeBlockedUserContent_withPostVotePresent_shouldDeletePostVote() {
    Post post = createPost("post1", null);
    PostVote vote = new PostVote();
    PostVoteId voteId = new PostVoteId(post.getPostId(), blockedUser.getId().toString());
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of());
    when(postVoteRepository.findById(voteId)).thenReturn(Optional.of(vote));

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(postVoteRepository).delete(vote);
  }

  @Test
  public void removeBlockedUserContent_withNullSource_shouldNotLookupSourceVote() {
    Post post = createPost("post1", null); // source is null
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of());
    when(postVoteRepository.findById(any())).thenReturn(Optional.empty());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(sourceVoteRepository, never()).findById(any());
  }

  @Test
  public void removeBlockedUserContent_withSourceVotePresent_shouldDeleteSourceVote() {
    Source source = createSource("src1");
    Post post = createPost("post1", source);
    SourceVote sourceVote = new SourceVote();
    SourceVoteId sourceVoteId = new SourceVoteId(source.getSourceId(), blockedUser.getId().toString());
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of());
    when(postVoteRepository.findById(any())).thenReturn(Optional.empty());
    when(sourceVoteRepository.findById(sourceVoteId)).thenReturn(Optional.of(sourceVote));

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(sourceVoteRepository).delete(sourceVote);
  }

  @Test
  public void removeBlockedUserContent_withSourceAndNoSourceVote_shouldNotDeleteSourceVote() {
    Source source = createSource("src1");
    Post post = createPost("post1", source);
    SourceVoteId sourceVoteId = new SourceVoteId(source.getSourceId(), blockedUser.getId().toString());
    stubNoRelationship();
    when(postRepository.findPostsByUser(currentUser)).thenReturn(List.of(post));
    when(commentRepository.findCommentsByUserAndPost(blockedUser, post)).thenReturn(List.of());
    when(postVoteRepository.findById(any())).thenReturn(Optional.empty());
    when(sourceVoteRepository.findById(sourceVoteId)).thenReturn(Optional.empty());

    blockService.blockUser(currentUser, blockedUser.getUsername());

    verify(sourceVoteRepository, never()).delete(any());
  }
  
  @Test
  public void getBlockedUsers_withValidUserButNullSearch_shouldReturnEmptyList() {
    when(blockedRelationRepository.findBlockedUserByUser(currentUser)).thenReturn(List.of());

    List<BlockedResponse> result = blockService.getBlockedUsers(currentUser, null);

    assertTrue(result.isEmpty());
    verify(blockedRelationRepository).findBlockedUserByUser(currentUser);
    verify(blockedRelationRepository, never()).findBlockedUserByUserAndSearch(any(), any());
    verify(relationshipMapper, never()).toBlockedResponse(any());
  }

  @Test
  public void getBlockedUsers_withBlankSearch_shouldUseDefaultRepositoryMethod() {
    when(blockedRelationRepository.findBlockedUserByUser(currentUser)).thenReturn(List.of());

    List<BlockedResponse> result = blockService.getBlockedUsers(currentUser, "   ");

    assertTrue(result.isEmpty());
    verify(blockedRelationRepository).findBlockedUserByUser(currentUser);
    verify(blockedRelationRepository, never()).findBlockedUserByUserAndSearch(any(), any());
  }

  @Test
  public void getBlockedUsers_withSearch_shouldUseSearchRepositoryAndMapResults() {
    BlockedResponse mapped =
        new BlockedResponse(blockedUser.getId(), blockedUser.getUsername(), blockedUser.getProfilePicture());
    when(blockedRelationRepository.findBlockedUserByUserAndSearch(currentUser, "blocked"))
        .thenReturn(List.of(blockedUser));
    when(relationshipMapper.toBlockedResponse(blockedUser)).thenReturn(mapped);

    List<BlockedResponse> result = blockService.getBlockedUsers(currentUser, "blocked");

    assertEquals(1, result.size());
    assertEquals(mapped, result.get(0));
    verify(blockedRelationRepository).findBlockedUserByUserAndSearch(currentUser, "blocked");
    verify(blockedRelationRepository, never()).findBlockedUserByUser(currentUser);
    verify(relationshipMapper).toBlockedResponse(blockedUser);
  }

  private void stubNoRelationship() {
    when(userRepository.findByUsername(blockedUser.getUsername())).thenReturn(Optional.of(blockedUser));
    when(blockedRelationRepository.existsBlockedRelationByUserAndBlockedUser(currentUser, blockedUser))
        .thenReturn(false);
    when(followService.isFollowing(currentUser, blockedUser)).thenReturn(false);
    when(followService.isFollowing(blockedUser, currentUser)).thenReturn(false);
    when(followService.areFriends(currentUser, blockedUser)).thenReturn(false);
    when(friendshipService.getIncomingRequests(currentUser)).thenReturn(List.of());
    when(friendshipService.getSentRequests(currentUser)).thenReturn(List.of());
  }

  private Post createPost(String postId, Source source) {
    Post post = new Post();
    post.setPostId(postId);
    post.setSource(source);
    return post;
  }

  private Comment createComment(String commentId) {
    Comment comment = new Comment();
    comment.setCommentId(commentId);
    return comment;
  }

  private Source createSource(String sourceId) {
    Source source = new Source();
    source.setSourceId(sourceId);
    return source;
  }

  private User createUser(String username) {
	User user = new User();
	user.setId(UUID.randomUUID());
	user.setUsername(username);
	user.setEmail(username + "@test.com");
	user.setName(username);
	return user;
  }
}
