package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.chat.entity.Chat;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.chat.entity.ChatMemberId;
import at.ac.tuwien.inso.ctrl.chat.entity.Message;
import at.ac.tuwien.inso.ctrl.chat.entity.Reaction;
import at.ac.tuwien.inso.ctrl.chat.entity.ReactionId;
import at.ac.tuwien.inso.ctrl.chat.entity.Shared;
import at.ac.tuwien.inso.ctrl.chat.repository.ChatRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.MessageRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.ReactionRepository;
import at.ac.tuwien.inso.ctrl.chat.repository.SharedRepository;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.dto.UpdatePasswordRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateProfileRequest;
import at.ac.tuwien.inso.ctrl.user.dto.UpdateUsernameRequest;
import at.ac.tuwien.inso.ctrl.user.dto.VerifyIdentityRequest;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerification;
import at.ac.tuwien.inso.ctrl.user.entity.BusinessVerificationStatus;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.BusinessVerificationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.IdAustriaOidcService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class UserControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private BusinessVerificationRepository businessVerificationRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private PostVoteRepository postVoteRepository;
  @Autowired private SourceRepository sourceRepository;
  @Autowired private SourceVoteRepository sourceVoteRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ChatRepository chatRepository;
  @Autowired private MessageRepository messageRepository;
  @Autowired private ReactionRepository reactionRepository;
  @Autowired private SharedRepository sharedRepository;
  @Autowired private EntityManager entityManager;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @MockitoBean private IdAustriaOidcService idAustriaOidcService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

  private User existingUser;
  private String validJwtToken;

  private final String meEndpoint = "/api/users/me";

  @BeforeEach
  public void setUp() {
    userRepository.deleteAll();

    existingUser =
        User.builder()
            .username("integrationUser")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Integration Test")
            .email("integration@test.com")
            .birthdate(LocalDate.of(1995, 5, 5))
            .emailVerified(true)
            .build();

    userRepository.save(existingUser);

    validJwtToken = jwtService.generateToken(existingUser);
  }

  @AfterEach
  public void tearDown() {
    businessVerificationRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void getUser_shouldReturnUserDetails_WhenAuthenticated() throws Exception {
    mockMvc
        .perform(get(meEndpoint).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("integrationUser"))
        .andExpect(jsonPath("$.email").value("integration@test.com"));
  }

  @Test
  public void getUser_shouldReturnUnauthorized_WhenNoTokenProvided() throws Exception {
    mockMvc.perform(get(meEndpoint)).andExpect(status().isUnauthorized());
  }

  @Test
  public void getPublicProfile_shouldReturnProfile_WhenUserExists() throws Exception {
    existingUser.setBio("Hello profile");
    existingUser.setProfilePicture("users/profile-pictures/avatar.png");
    userRepository.save(existingUser);

    mockMvc
        .perform(
            get("/api/users/integrationUser")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("integrationUser"))
        .andExpect(jsonPath("$.bio").value("Hello profile"))
        .andExpect(jsonPath("$.profilePicture").value("users/profile-pictures/avatar.png"));
  }

  @Test
  public void getPublicProfile_shouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
    mockMvc
        .perform(
            get("/api/users/missingUser")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isNotFound());
  }

  @Test
  public void deleteCurrentUser_shouldAnonymizeUserAndKeepChatHistory() throws Exception {
    User otherUser =
        userRepository.save(
            User.builder()
                .username("otherUser")
                .password(passwordEncoder.encode("securePass123!"))
                .name("Other User")
                .email("other@test.com")
                .birthdate(LocalDate.of(1994, 4, 4))
                .emailVerified(true)
                .build());
    String otherToken = jwtService.generateToken(otherUser);
    LocalDateTime now = LocalDateTime.now();

    Post deletedUserPost =
        postRepository.save(
            Post.builder()
                .postId("deleted-user-post")
                .user(existingUser)
                .caption("private content")
                .mediaRef("posts/media/deleted.png")
                .isPublic(true)
                .isRemoved(false)
                .upvotes(0)
                .downvotes(0)
                .createdAt(now)
                .build());
    Post otherPost =
        postRepository.save(
            Post.builder()
                .postId("other-post")
                .user(otherUser)
                .caption("other content")
                .isPublic(true)
                .isRemoved(false)
                .upvotes(1)
                .downvotes(0)
                .createdAt(now)
                .build());
    Source source =
        sourceRepository.save(
            Source.builder()
                .sourceId("account-delete-source")
                .upvotes(1)
                .downvotes(0)
                .isRemoved(false)
                .createdAt(now)
                .build());
    postVoteRepository.save(
        PostVote.builder()
            .id(new PostVoteId(otherPost.getPostId(), existingUser.getId().toString()))
            .post(otherPost)
            .voteType("up")
            .build());
    sourceVoteRepository.save(
        SourceVote.builder()
            .id(new SourceVoteId(source.getSourceId(), existingUser.getId().toString()))
            .source(source)
            .voteType("up")
            .build());
    Comment comment =
        commentRepository.save(
            Comment.builder()
                .commentId("comment-from-deleted-user")
                .post(otherPost)
                .user(existingUser)
                .content("delete this")
                .createdAt(now)
                .isRemoved(false)
                .build());
    Comment reply =
        commentRepository.save(
            Comment.builder()
                .commentId("reply-to-deleted-user-comment")
                .post(otherPost)
                .user(otherUser)
                .content("reply should disappear too")
                .createdAt(now.plusSeconds(1))
                .isRemoved(false)
                .parentComment(comment)
                .build());

    UUID chatId = UUID.randomUUID();
    Chat chat = Chat.builder().chatId(chatId).createdAt(now).build();
    chat.getMembers()
        .add(
            ChatMember.builder()
                .id(new ChatMemberId(chatId, existingUser.getId()))
                .chat(chat)
                .user(existingUser)
                .joinedAt(now)
                .build());
    chat.getMembers()
        .add(
            ChatMember.builder()
                .id(new ChatMemberId(chatId, otherUser.getId()))
                .chat(chat)
                .user(otherUser)
                .joinedAt(now)
                .build());
    chatRepository.save(chat);

    UUID groupChatId = UUID.randomUUID();
    Chat groupChat =
        Chat.builder()
            .chatId(groupChatId)
            .name("account-delete-group")
            .createdAt(now)
            .owner(existingUser)
            .build();
    groupChat
        .getMembers()
        .add(
            ChatMember.builder()
                .id(new ChatMemberId(groupChatId, existingUser.getId()))
                .chat(groupChat)
                .user(existingUser)
                .joinedAt(now)
                .build());
    groupChat
        .getMembers()
        .add(
            ChatMember.builder()
                .id(new ChatMemberId(groupChatId, otherUser.getId()))
                .chat(groupChat)
                .user(otherUser)
                .joinedAt(now)
                .build());
    chatRepository.save(groupChat);

    Message historicalMessage =
        messageRepository.save(
            Message.builder()
                .messageId(UUID.randomUUID())
                .chat(chat)
                .fromUser(existingUser)
                .content("historical message")
                .date(now)
                .system(false)
                .build());
    Shared sharedPost =
        sharedRepository.save(
            Shared.builder()
                .sharedId(UUID.randomUUID())
                .post(deletedUserPost)
                .sharedBy(otherUser)
                .createdAt(now)
                .build());
    Message sharedMessage =
        messageRepository.save(
            Message.builder()
                .messageId(UUID.randomUUID())
                .chat(chat)
                .fromUser(otherUser)
                .content("")
                .date(now.plusMinutes(1))
                .system(false)
                .shared(sharedPost)
                .build());
    reactionRepository.save(
        Reaction.builder()
            .id(new ReactionId(existingUser.getId(), sharedMessage.getMessageId(), "👍"))
            .message(sharedMessage)
            .fromUser(existingUser)
            .content("👍")
            .createdAt(now)
            .build());

    mockMvc
        .perform(delete(meEndpoint).header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isNoContent());

    entityManager.flush();
    entityManager.clear();

    User deletedUser = userRepository.findById(existingUser.getId()).orElseThrow();
    assertTrue(deletedUser.isDeleted());
    assertTrue(deletedUser.getUsername().startsWith("deleted-user-"));
    assertEquals("Deleted user", deletedUser.getName());
    assertNull(deletedUser.getProfilePicture());
    assertTrue(postRepository.findById(deletedUserPost.getPostId()).orElseThrow().getIsRemoved());

    assertFalse(commentRepository.findById(comment.getCommentId()).isPresent());
    assertFalse(commentRepository.findById(reply.getCommentId()).isPresent());
    assertFalse(
        postVoteRepository
            .findById(new PostVoteId(otherPost.getPostId(), existingUser.getId().toString()))
            .isPresent());
    assertFalse(
        sourceVoteRepository
            .findById(new SourceVoteId(source.getSourceId(), existingUser.getId().toString()))
            .isPresent());
    assertEquals(0, postRepository.findById(otherPost.getPostId()).orElseThrow().getUpvotes());
    assertEquals(0, sourceRepository.findById(source.getSourceId()).orElseThrow().getUpvotes());
    assertEquals(0, reactionRepository.count());

    mockMvc
        .perform(
            get("/api/users/integrationUser")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isNotFound());

    mockMvc
        .perform(
            get("/api/chats/" + chatId + "/messages")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].messageId").value(historicalMessage.getMessageId().toString()))
        .andExpect(jsonPath("$[0].sender.username").value("Deleted user"))
        .andExpect(jsonPath("$[1].messageId").value(sharedMessage.getMessageId().toString()))
        .andExpect(jsonPath("$[1].sharedPost.canView").value(false))
        .andExpect(jsonPath("$[1].sharedPost.unavailableReason").value("REMOVED"));

    List<Message> groupMessages =
        messageRepository.findByChatOrderByDateAsc(
            chatRepository.findById(groupChatId).orElseThrow());
    assertTrue(
        groupMessages.stream()
            .anyMatch(
                message ->
                    message.isSystem()
                        && "Deleted user deleted their account and left the group."
                            .equals(message.getContent())));
    assertTrue(
        groupMessages.stream()
            .anyMatch(
                message ->
                    message.isSystem()
                        && "All remaining members are now group owners."
                            .equals(message.getContent())));
  }

  @Test
  public void searchUsers_shouldReturnProfilePictureUrl_WhenUserHasProfilePicture()
      throws Exception {
    User targetUser =
        User.builder()
            .username("marlene")
            .password(passwordEncoder.encode("securePass123!"))
            .name("Marlene")
            .email("marlene@test.com")
            .birthdate(LocalDate.of(1997, 7, 7))
            .profilePicture("users/profile-pictures/marlene.png")
            .emailVerified(true)
            .isVerified(true)
            .build();
    userRepository.save(targetUser);

    mockMvc
        .perform(
            post("/api/search/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"query\":\"mar\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].username").value("marlene"))
        .andExpect(jsonPath("$[0].profilePicture").value("users/profile-pictures/marlene.png"));
  }

  @Test
  public void updateUser_shouldUpdateProfile_WhenRequestIsValid() throws Exception {
    UpdateProfileRequest request =
        new UpdateProfileRequest(
            "Updated Name", "updated@test.com", "New bio", LocalDate.of(1996, 6, 6));

    mockMvc
        .perform(
            put(meEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Name"))
        .andExpect(jsonPath("$.email").value("updated@test.com"));

    User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
    assertEquals("updated@test.com", dbUser.getEmail());
  }

  @Test
  public void updateUser_shouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
    User secondUser =
        User.builder()
            .username("secondUser")
            .password("pass")
            .email("taken@test.com")
            .name("Second")
            .build();
    userRepository.save(secondUser);

    UpdateProfileRequest request =
        new UpdateProfileRequest(
            "Updated Name", "taken@test.com", "New bio", LocalDate.of(1995, 5, 5));

    mockMvc
        .perform(
            put(meEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isConflict());
  }

  @Test
  public void updatePassword_shouldReturnNoContent_WhenOldPasswordIsCorrect() throws Exception {
    UpdatePasswordRequest request =
        new UpdatePasswordRequest("securePass123!", "newSecurePass123!");

    mockMvc
        .perform(
            put(meEndpoint + "/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());

    // Verify the new password hash is stored in the DB
    User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
    assertTrue(passwordEncoder.matches("newSecurePass123!", dbUser.getPassword()));
  }

  @Test
  public void updateUsername_shouldReturnNewToken_WhenUsernameIsAvailable() throws Exception {
    UpdateUsernameRequest request = new UpdateUsernameRequest("newAwesomeUsername");

    mockMvc
        .perform(
            put(meEndpoint + "/username")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty());

    User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
    assertEquals("newAwesomeUsername", dbUser.getUsername());
  }

  @Test
  public void verifyUser_shouldUpdateVerifiedStatus_WhenIdAustriaSucceeds() throws Exception {
    existingUser.setVerified(false);
    userRepository.save(existingUser);

    VerifyIdentityRequest request = new VerifyIdentityRequest("valid-code");

    // Mock the external ID Austria JWT response
    Jwt mockJwt = mock(Jwt.class);
    when(idAustriaOidcService.getTokenFromCode("valid-code")).thenReturn(mockJwt);
    when(mockJwt.getClaimAsString("given_name")).thenReturn("Official");
    when(mockJwt.getClaimAsString("family_name")).thenReturn("Person");
    when(mockJwt.getClaimAsString("birthdate")).thenReturn("1980-08-08");

    mockMvc
        .perform(
            put(meEndpoint + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.isVerified").value(true));

    User dbUser = userRepository.findById(existingUser.getId()).orElseThrow();
    assertTrue(dbUser.isVerified());
    assertEquals("Official Person", dbUser.getName());
  }

  @Test
  public void submitBusinessVerification_shouldCreatePendingVerification_WhenCalled()
      throws Exception {
    mockMvc
        .perform(
            post(meEndpoint + "/verify/business")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk());
    BusinessVerification verification =
        businessVerificationRepository.findByBusinessUser(existingUser);

    assertNotNull(verification);
    assertEquals(verification.getBusinessUser(), existingUser);
    assertEquals(BusinessVerificationStatus.PENDING, verification.getStatus());
  }

  @Test
  public void getVerificationStatus_shouldReturnPendingVerification_WhenCalled() throws Exception {
    BusinessVerification verification =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.PENDING)
            .businessUser(existingUser)
            .build();
    businessVerificationRepository.save(verification);

    mockMvc
        .perform(
            get(meEndpoint + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("BUSINESS"))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  public void getVerificationStatus_shouldReturnApprovedVerification_WhenCalled() throws Exception {
    BusinessVerification verification =
        BusinessVerification.builder()
            .status(BusinessVerificationStatus.APPROVED)
            .businessUser(existingUser)
            .build();
    businessVerificationRepository.save(verification);

    mockMvc
        .perform(
            get(meEndpoint + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("BUSINESS"))
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }

  @Test
  public void getVerificationStatus_shouldReturnNotVerified_WhenCalled() throws Exception {

    mockMvc
        .perform(
            get(meEndpoint + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("PERSONAL"))
        .andExpect(jsonPath("$.status").value("NOT_VERIFIED"));
  }

  @Test
  public void getVerificationStatus_shouldReturnVerified_WhenCalled() throws Exception {
    existingUser.setVerified(true);

    userRepository.save(existingUser);
    mockMvc
        .perform(
            get(meEndpoint + "/verify")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("PERSONAL"))
        .andExpect(jsonPath("$.status").value("APPROVED"));
  }
}
