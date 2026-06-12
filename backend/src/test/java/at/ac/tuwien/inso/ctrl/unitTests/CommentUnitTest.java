package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import at.ac.tuwien.inso.ctrl.comments.dto.request.CreateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.request.UpdateCommentRequest;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.mapper.CommentMapper;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.comments.service.impl.CommentServiceImpl;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.BlockService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class CommentUnitTests {

  @Mock private CommentRepository commentRepository;
  @Mock private PostRepository postRepository;
  @Mock private UserRepository userRepository;
  @Mock private CommentMapper commentMapper;
  @Mock private BlockService blockService;

  @InjectMocks private CommentServiceImpl commentService;

  /** getCommentsForPost - input validation * */
  @ParameterizedTest
  @MethodSource("invalidGetCommentsInputs")
  void getCommentsForPost_shouldThrow_whenInputIsInvalid(String postId, HttpStatus expectedStatus) {
    User currentUser = buildUser("alice");
    when(postRepository.existsById(postId)).thenReturn(false);

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> commentService.getCommentsForPost(postId, currentUser));

    assertEquals(expectedStatus, ex.getStatusCode());
  }

  static Stream<Arguments> invalidGetCommentsInputs() {
    return Stream.of(
        Arguments.of("unknown-post", HttpStatus.NOT_FOUND), // post does not exist
        Arguments.of("", HttpStatus.NOT_FOUND) // another non-existing post
        );
  }

  @ParameterizedTest
  @MethodSource("validGetCommentsInputs")
  void getCommentsForPost_shouldNotThrow_whenInputIsValid(String postId) {
    User currentUser = buildUser("alice");
    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.findByPost_PostId(postId)).thenReturn(List.of());

    assertDoesNotThrow(() -> commentService.getCommentsForPost(postId, currentUser));
  }

  static Stream<Arguments> validGetCommentsInputs() {
    return Stream.of(Arguments.of("post-1"), Arguments.of("post-2"));
  }

  /** getCommentsForPost - behavior * */
  @Test
  void getCommentsForPost_shouldReturnOnlyTopLevelComments() {
    String postId = "post-1";
    User currentUser = buildUser("alice");
    User topLevelAuthor = buildUser("bob");
    User replyAuthor = buildUser("charlie");

    Comment topLevel = buildComment("c-1", null);
    topLevel.setUser(topLevelAuthor);

    Comment reply = buildComment("c-2", topLevel);
    reply.setUser(replyAuthor);

    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.findByPost_PostId(postId)).thenReturn(List.of(topLevel, reply));
    when(blockService.inBlockedRelationship(currentUser, topLevelAuthor)).thenReturn(false);
    when(blockService.inBlockedRelationship(currentUser, replyAuthor)).thenReturn(false);
    when(commentMapper.toResponse(eq(topLevel), any())).thenReturn(mock(CommentResponse.class));

    List<CommentResponse> result = commentService.getCommentsForPost(postId, currentUser);

    assertEquals(1, result.size());
    // Mapper must only be called for top-level comment, not for the reply
    verify(commentMapper, times(1)).toResponse(any(), any());
  }

  @Test
  void getCommentsForPost_shouldExcludeCommentsFromBlockedUsers() {
    String postId = "post-1";
    User currentUser = buildUser("alice");
    User blockedAuthor = buildUser("bob");
    Comment blockedComment = buildComment("c-1", null);
    blockedComment.setUser(blockedAuthor);

    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.findByPost_PostId(postId)).thenReturn(List.of(blockedComment));
    when(blockService.inBlockedRelationship(currentUser, blockedAuthor)).thenReturn(true);

    List<CommentResponse> result = commentService.getCommentsForPost(postId, currentUser);

    assertTrue(result.isEmpty());
    verify(commentMapper, never()).toResponse(any(), any());
  }

  @Test
  void getCommentsForPost_shouldReturnEmptyList_whenNoCommentsExist() {
    String postId = "post-1";
    User currentUser = buildUser("alice");
    when(postRepository.existsById(postId)).thenReturn(true);
    when(commentRepository.findByPost_PostId(postId)).thenReturn(List.of());

    List<CommentResponse> result = commentService.getCommentsForPost(postId, currentUser);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  /** addComment - input validation * */
  @ParameterizedTest
  @MethodSource("invalidAddCommentInputs")
  void addComment_shouldThrow_whenInputIsInvalid(
      String postId, String username, String parentCommentId, HttpStatus expectedStatus) {
    CreateCommentRequest request = mock(CreateCommentRequest.class);
    lenient().when(request.parentCommentId()).thenReturn(parentCommentId);

    // post not found
    if (expectedStatus == HttpStatus.NOT_FOUND && username == null) {
      when(postRepository.findById(postId)).thenReturn(Optional.empty());
    }

    // post found, user not found
    if (expectedStatus == HttpStatus.NOT_FOUND && username != null && parentCommentId == null) {
      when(postRepository.findById(postId)).thenReturn(Optional.of(buildPost(postId)));
      when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
    }

    // post + user found, parent comment not found
    if (expectedStatus == HttpStatus.NOT_FOUND && parentCommentId != null) {
      when(postRepository.findById(postId)).thenReturn(Optional.of(buildPost(postId)));
      when(userRepository.findByUsername(username)).thenReturn(Optional.of(buildUser(username)));
      when(commentRepository.findById(parentCommentId)).thenReturn(Optional.empty());
    }

    // post + user + parent comment found, but parent belongs to different post
    if (expectedStatus == HttpStatus.BAD_REQUEST) {
      Post differentPost = buildPost("different-post");
      Comment parentComment = buildComment(parentCommentId, null);
      parentComment.setPost(differentPost);

      when(postRepository.findById(postId)).thenReturn(Optional.of(buildPost(postId)));
      when(userRepository.findByUsername(username)).thenReturn(Optional.of(buildUser(username)));
      when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parentComment));
    }

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> commentService.addComment(postId, request, username));

    assertEquals(expectedStatus, ex.getStatusCode());
  }

  static Stream<Arguments> invalidAddCommentInputs() {
    return Stream.of(
        Arguments.of("unknown-post", null, null, HttpStatus.NOT_FOUND), // post not found
        Arguments.of("post-1", "ghost", null, HttpStatus.NOT_FOUND), // user not found
        Arguments.of(
            "post-1", "alice", "unknown", HttpStatus.NOT_FOUND), // parent comment not found
        Arguments.of(
            "post-1",
            "alice",
            "c-other",
            HttpStatus.BAD_REQUEST) // parent belongs to different post
        );
  }

  @ParameterizedTest
  @MethodSource("validAddCommentInputs")
  void addComment_shouldNotThrow_whenInputIsValid(
      String postId, String username, String parentCommentId) {
    CreateCommentRequest request = mock(CreateCommentRequest.class);
    when(request.parentCommentId()).thenReturn(parentCommentId);
    when(request.content()).thenReturn("some content");

    when(postRepository.findById(postId)).thenReturn(Optional.of(buildPost(postId)));
    when(userRepository.findByUsername(username)).thenReturn(Optional.of(buildUser(username)));

    if (parentCommentId != null) {
      Comment parent = buildComment(parentCommentId, null);
      parent.setPost(buildPost(postId)); // same post
      when(commentRepository.findById(parentCommentId)).thenReturn(Optional.of(parent));
    }

    when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(commentMapper.toResponse(any(), any())).thenReturn(mock(CommentResponse.class));

    assertDoesNotThrow(() -> commentService.addComment(postId, request, username));
  }

  static Stream<Arguments> validAddCommentInputs() {
    return Stream.of(
        Arguments.of("post-1", "alice", null), // top-level comment
        Arguments.of("post-1", "alice", "c-1") // reply to existing comment
        );
  }

  /** updateComment - input validation * */
  @ParameterizedTest
  @MethodSource("invalidUpdateCommentInputs")
  void updateComment_shouldThrow_whenInputIsInvalid(
      String commentId, String username, HttpStatus expectedStatus) {
    UpdateCommentRequest request = mock(UpdateCommentRequest.class);

    // comment not found
    if (expectedStatus == HttpStatus.NOT_FOUND) {
      when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
    }

    // comment found but user is not the author
    if (expectedStatus == HttpStatus.FORBIDDEN) {
      Comment comment = buildComment(commentId, null);
      comment.setUser(buildUser("other-user"));
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    }

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class,
            () -> commentService.updateComment(commentId, request, username));

    assertEquals(expectedStatus, ex.getStatusCode());
  }

  static Stream<Arguments> invalidUpdateCommentInputs() {
    return Stream.of(
        Arguments.of("unknown-comment", "alice", HttpStatus.NOT_FOUND), // comment not found
        Arguments.of("c-1", "alice", HttpStatus.FORBIDDEN) // user is not author
        );
  }

  @ParameterizedTest
  @MethodSource("validUpdateCommentInputs")
  void updateComment_shouldNotThrow_whenInputIsValid(String commentId, String username) {
    UpdateCommentRequest request = mock(UpdateCommentRequest.class);
    when(request.content()).thenReturn("updated content");

    Comment comment = buildComment(commentId, null);
    comment.setUser(buildUser(username)); // same user → allowed

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(commentMapper.toResponse(any(), any())).thenReturn(mock(CommentResponse.class));

    assertDoesNotThrow(() -> commentService.updateComment(commentId, request, username));
  }

  static Stream<Arguments> validUpdateCommentInputs() {
    return Stream.of(Arguments.of("c-1", "alice"), Arguments.of("c-2", "bob"));
  }

  /** deleteComment - input validation * */
  @ParameterizedTest
  @MethodSource("invalidDeleteCommentInputs")
  void deleteComment_shouldThrow_whenInputIsInvalid(
      String commentId, String username, HttpStatus expectedStatus) {
    // comment not found
    if (expectedStatus == HttpStatus.NOT_FOUND) {
      when(commentRepository.findById(commentId)).thenReturn(Optional.empty());
    }

    // comment found but user is not the author
    if (expectedStatus == HttpStatus.FORBIDDEN) {
      Comment comment = buildComment(commentId, null);
      comment.setUser(buildUser("other-user"));
      when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    }

    ResponseStatusException ex =
        assertThrows(
            ResponseStatusException.class, () -> commentService.deleteComment(commentId, username));

    assertEquals(expectedStatus, ex.getStatusCode());
  }

  static Stream<Arguments> invalidDeleteCommentInputs() {
    return Stream.of(
        Arguments.of("unknown-comment", "alice", HttpStatus.NOT_FOUND), // comment not found
        Arguments.of("c-1", "alice", HttpStatus.FORBIDDEN) // user is not author
        );
  }

  @ParameterizedTest
  @MethodSource("validDeleteCommentInputs")
  void deleteComment_shouldNotThrow_whenInputIsValid(String commentId, String username) {
    Comment comment = buildComment(commentId, null);
    comment.setUser(buildUser(username)); // same user → allowed

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    doNothing().when(commentRepository).delete(comment);

    assertDoesNotThrow(() -> commentService.deleteComment(commentId, username));
  }

  static Stream<Arguments> validDeleteCommentInputs() {
    return Stream.of(Arguments.of("c-1", "alice"), Arguments.of("c-2", "bob"));
  }

  /** deleteComment - behavior * */
  @Test
  void deleteComment_shouldCallRepositoryDelete() {
    String commentId = "c-1";
    String username = "alice";
    Comment comment = buildComment(commentId, null);
    comment.setUser(buildUser(username));

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));

    commentService.deleteComment(commentId, username);

    verify(commentRepository).delete(comment);
  }

  /** updateComment - behavior * */
  @Test
  void updateComment_shouldSetEditedTrue_andUpdateContent() {
    String commentId = "c-1";
    String username = "alice";
    String newContent = "updated content";

    UpdateCommentRequest request = mock(UpdateCommentRequest.class);
    when(request.content()).thenReturn(newContent);

    Comment comment = buildComment(commentId, null);
    comment.setUser(buildUser(username));

    when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
    when(commentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(commentMapper.toResponse(any(), eq(Map.of()))).thenReturn(mock(CommentResponse.class));

    commentService.updateComment(commentId, request, username);

    assertTrue(comment.isEdited());
    assertEquals(newContent, comment.getContent());
  }

  /** Helpers * */
  private Post buildPost(String postId) {
    Post post = new Post();
    post.setPostId(postId);
    return post;
  }

  private User buildUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private Comment buildComment(String commentId, Comment parentComment) {
    return Comment.builder().commentId(commentId).parentComment(parentComment).build();
  }
}
