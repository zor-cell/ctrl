package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.scheduler.MastodonScheduler;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.RelationshipMapper;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "spring.task.scheduling.enabled=false")
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class PostControllerDatabaseIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;

  @Autowired private FollowRelationRepository followRelationRepository;

  @Autowired private FriendRelationRepository friendRelationRepository;

  @Autowired private TopicRepository topicRepository;

  @Autowired private PostRepository postRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @MockitoBean private EmbeddingService embeddingService;

  @MockitoBean private RelationshipMapper relationshipMapper;

  @MockitoBean private MastodonScheduler mastodonScheduler;

  @MockitoBean private FileStorageService fileStorageService;

  private User verifiedUser;
  private User otherUser;
  private String verifiedUserToken;
  private String otherUserToken;
  private String generatedTopicId;

  @BeforeEach
  void setUp() {
    followRelationRepository.deleteAll();
    friendRelationRepository.deleteAll();
    postRepository.deleteAll();
    topicRepository.deleteAll();
    userRepository.deleteAll();

    verifiedUser = saveUser("marlene", true);
    otherUser = saveUser("other", true);
    Topic savedTopic = topicRepository.save(Topic.builder().name("Politics").emoji("P").build());
    generatedTopicId = savedTopic.getTopicId();
    verifiedUserToken = jwtService.generateToken(verifiedUser);
    otherUserToken = jwtService.generateToken(otherUser);
    when(fileStorageService.resolveFileUrl(anyString()))
        .thenAnswer(
            invocation ->
                "http://localhost:8080/api/files/media-url?ref="
                    + invocation.getArgument(0, String.class).replace("/", "%2F"));
  }

  @AfterEach
  void tearDown() {
    followRelationRepository.deleteAll();
    friendRelationRepository.deleteAll();
    postRepository.deleteAll();
    topicRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void createPost_withValidRequest_persistsPostAndReturnsCreated() throws Exception {
    when(embeddingService.embedText("A valid caption")).thenReturn(new float[768]);

    mockMvc
        .perform(
            post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + verifiedUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caption": "A valid caption",
                      "mediaRef": "posts/media/file.png",
                      "isPublic": true,
                      "isAiGenerated": false,
                      "topicIds": ["%s"],
                      "source": null
                    }
                    """
                        .formatted(generatedTopicId)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("marlene"))
        .andExpect(jsonPath("$.caption").value("A valid caption"))
        .andExpect(jsonPath("$.mediaRef").value("posts/media/file.png"))
        .andExpect(
            jsonPath("$.mediaUrl")
                .value("http://localhost:8080/api/files/media-url?ref=posts%2Fmedia%2Ffile.png"))
        .andExpect(jsonPath("$.isPublic").value(true))
        .andExpect(jsonPath("$.topicIds[0]").value(generatedTopicId));

    var savedPosts = postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene");
    assertEquals(1, savedPosts.size());
    assertEquals("A valid caption", savedPosts.get(0).getCaption());
    assertTrue(savedPosts.get(0).isPublic());
    assertEquals("posts/media/file.png", savedPosts.get(0).getMediaRef());
  }

  @Test
  void createPost_withBlankCaption_returnsBadRequestAndDoesNotPersist() throws Exception {
    mockMvc
        .perform(
            post("/api/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + verifiedUserToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caption": "   ",
                      "mediaRef": null,
                      "isPublic": false,
                      "isAiGenerated": false,
                      "topicIds": [],
                      "source": null
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().string(containsString("caption must not be blank")));

    assertTrue(postRepository.findAllByUserUsernameOrderByCreatedAtDesc("marlene").isEmpty());
  }

  @Test
  void getProfilePosts_withExistingPosts_returnsPublicNewestPostsFirst() throws Exception {
    savePost("private-post", "Private", false, LocalDateTime.of(2026, 4, 25, 13, 0));
    savePost("older-post", "Older", true, LocalDateTime.of(2026, 4, 25, 10, 0));
    savePost("newer-post", "Newer", true, LocalDateTime.of(2026, 4, 25, 12, 0));

    mockMvc
        .perform(
            get("/api/posts/user/marlene")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].postId").value("newer-post"))
        .andExpect(jsonPath("$[0].caption").value("Newer"))
        .andExpect(jsonPath("$[0].isPublic").value(true))
        .andExpect(jsonPath("$[1].postId").value("older-post"))
        .andExpect(jsonPath("$[1].isPublic").value(true))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void getProfilePosts_withFriendRelation_returnsPrivateAndPublicPosts() throws Exception {
    saveFriendRelation(otherUser, verifiedUser);
    savePost("private-post", "Private", false, LocalDateTime.of(2026, 4, 25, 13, 0));
    savePost("public-post", "Public", true, LocalDateTime.of(2026, 4, 25, 12, 0));

    mockMvc
        .perform(
            get("/api/posts/user/marlene")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[*].postId", containsInAnyOrder("private-post", "public-post")))
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  void deletePost_withOwnPost_deletesPost() throws Exception {
    savePost("post-1", "Delete me", false, LocalDateTime.of(2026, 4, 25, 12, 0));

    mockMvc
        .perform(
            delete("/api/posts/post-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + verifiedUserToken))
        .andExpect(status().isNoContent());

    assertFalse(postRepository.findById("post-1").isPresent());
  }

  @Test
  void deletePost_withForeignPost_returnsNotFoundAndKeepsPost() throws Exception {
    savePost("post-1", "Do not delete me", false, LocalDateTime.of(2026, 4, 25, 12, 0));

    mockMvc
        .perform(
            delete("/api/posts/post-1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherUserToken))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Post not found"));

    assertTrue(postRepository.findById("post-1").isPresent());
  }

  private User saveUser(String username, boolean verified) {
    return userRepository.save(
        User.builder()
            .username(username)
            .password(passwordEncoder.encode("securePass123!"))
            .name(username)
            .email(username + "@test.com")
            .birthdate(LocalDate.of(1995, 5, 5))
            .emailVerified(true)
            .isVerified(verified)
            .build());
  }

  private Post savePost(String postId, String caption, boolean isPublic, LocalDateTime createdAt) {
    return postRepository.save(
        Post.builder()
            .postId(postId)
            .user(verifiedUser)
            .caption(caption)
            .mediaRef("posts/media/" + postId + ".png")
            .isPublic(isPublic)
            .isAiGenerated(false)
            .upvotes(0)
            .downvotes(0)
            .createdAt(createdAt)
            .embedding(new float[768])
            .build());
  }

  private void saveFollowRelation(User follower, User followed) {
    followRelationRepository.save(
        FollowRelation.builder()
            .id(new FollowRelationId(follower.getId(), followed.getId()))
            .follower(follower)
            .followed(followed)
            .since(LocalDateTime.now())
            .build());
  }

  private void saveFriendRelation(User firstUser, User secondUser) {
    friendRelationRepository.save(
        FriendRelation.builder()
            .id(new FriendRelationId(firstUser.getId(), secondUser.getId()))
            .friend1(firstUser)
            .friend2(secondUser)
            .since(LocalDateTime.now())
            .build());
  }
}
