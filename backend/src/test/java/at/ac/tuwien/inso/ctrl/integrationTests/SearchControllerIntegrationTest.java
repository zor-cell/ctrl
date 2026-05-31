package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostPreviewResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.dto.RelationshipStatusItem;
import at.ac.tuwien.inso.ctrl.user.dto.UserSearchResponse;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FollowRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelation;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRelationId;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequest;
import at.ac.tuwien.inso.ctrl.user.entity.FriendRequestId;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRequestRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class SearchControllerIntegrationTest {

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private UserRepository userRepository;
  @Autowired private FollowRelationRepository followRelationRepository;
  @Autowired private FriendRelationRepository friendRelationRepository;
  @Autowired private FriendRequestRepository friendRequestRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private TopicRepository topicRepository;
  @PersistenceContext private EntityManager entityManager;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private MockMvc mockMvc;

  @BeforeEach
  public void setup() {
    mockMvc = webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
  }

  @AfterEach
  public void tearDown() {
    postRepository.deleteAll();
    followRelationRepository.deleteAll();
    friendRelationRepository.deleteAll();
    friendRequestRepository.deleteAll();
    topicRepository.deleteAll();
    userRepository.deleteAll();
  }

  // ==================== USER SEARCH TESTS ====================

  @Nested
  @Transactional
  class UserSearchTests {

    private User currentUser;
    private User bert;
    private User dagobert;
    private User bart;
    private User bertram;

    @BeforeEach
    void setUp() {
      currentUser =
          User.builder()
              .username("currentuser")
              .email("current@test.com")
              .password("password123")
              .name("Current User")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      currentUser = userRepository.save(currentUser);

      bert =
          User.builder()
              .username("bert")
              .email("bert@test.com")
              .password("password123")
              .name("Bert")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      bert = userRepository.save(bert);

      dagobert =
          User.builder()
              .username("dagobert")
              .email("dagobert@test.com")
              .password("password123")
              .name("Dagobert Duck")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      dagobert = userRepository.save(dagobert);

      bart =
          User.builder()
              .username("bart")
              .email("bart@test.com")
              .password("password123")
              .name("Bart")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      bart = userRepository.save(bart);

      bertram =
          User.builder()
              .username("bertram")
              .email("bertram@test.com")
              .password("password123")
              .name("Bertram")
              .isVerified(false)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      bertram = userRepository.save(bertram);
    }

    @Test
    public void searchUsers_basicSearch_shouldReturnMatching() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
    }

    @Test
    public void searchUsers_caseInsensitive_shouldWork() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"BERT\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
    }

    @Test
    public void searchUsers_shouldExcludeSelf() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"current\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(responses.stream().anyMatch(r -> r.username().equals("currentuser")));
    }

    @Test
    public void searchUsers_emptyQuery_shouldReturnBadRequest() throws Exception {
      mockMvc
          .perform(
              post("/api/search/users")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      "{\"query\":\"\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                  .with(user(currentUser)))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void searchUsers_noMatches_shouldReturnEmptyList() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"ghost123\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertTrue(responses.isEmpty());
    }

    @Test
    public void searchUsers_excludeFriends_shouldFilterFriends() throws Exception {
      createFriendship(currentUser, bert);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":true,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    }

    @Test
    public void searchUsers_excludeFollowed_shouldFilterFollowed() throws Exception {
      createFollow(currentUser, dagobert);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":true,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    }

    @Test
    public void searchUsers_verifiedOnly_shouldFilterUnverified() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":true}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(2, responses.size());
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    }

    @Test
    public void searchUsers_combinedFilters_shouldWorkTogether() throws Exception {
      createFriendship(currentUser, dagobert);
      createFollow(currentUser, bart);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":true,\"excludeFollowed\":true,\"verifiedOnly\":true}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertTrue(responses.stream().anyMatch(r -> r.username().equals("bert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("dagobert")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bart")));
      assertFalse(responses.stream().anyMatch(r -> r.username().equals("bertram")));
    }

    @Test
    public void searchUsers_statusFriend_shouldBeCorrect() throws Exception {
      createFriendship(currentUser, dagobert);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"dago\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertTrue(hasStatus(responses.get(0), RelationshipStatusItem.RelationshipStatus.FRIEND));
    }

    @Test
    public void searchUsers_statusFollowed_shouldBeCorrect() throws Exception {
      createFollow(currentUser, bertram);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"ram\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertTrue(hasStatus(responses.get(0), RelationshipStatusItem.RelationshipStatus.FOLLOWED));
    }

    @Test
    public void searchUsers_statusPendingSent_shouldBeCorrect() throws Exception {
      createFriendRequest(currentUser, bart);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bart\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertTrue(
          hasStatus(
              responses.get(0), RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_SENT));
    }

    @Test
    public void searchUsers_statusPendingIncoming_shouldBeCorrect() throws Exception {
      createFriendRequest(dagobert, currentUser);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"dag\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertTrue(
          hasStatus(
              responses.get(0),
              RelationshipStatusItem.RelationshipStatus.PENDING_REQUEST_INCOMING));
    }

    @Test
    public void searchUsers_statusNone_shouldBeCorrect() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(3, responses.size());
      assertTrue(responses.stream().allMatch(r -> r.relationshipStatuses().isEmpty()));
    }

    @Test
    public void searchUsers_displayName_verifiedFriend_shouldShowRealName() throws Exception {
      createFriendship(currentUser, dagobert);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"dagobert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertEquals("Dagobert Duck", responses.get(0).name());
    }

    @Test
    public void searchUsers_displayName_unverifiedNonFriend_shouldBeNull() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bertram\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertNull(responses.get(0).name());
    }

    @Test
    public void searchUsers_displayName_verifiedNonFriend_shouldShowReal() throws Exception {
      createFollow(currentUser, dagobert);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"obert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertEquals("Dagobert Duck", responses.get(0).name());
    }

    @Test
    public void searchUsers_displayName_unverifiedFriend_shouldShowReal() throws Exception {
      createFriendship(currentUser, bertram);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bertr\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      assertEquals("Bertram", responses.get(0).name());
    }

    @Test
    public void searchUsers_returnsCorrectFields() throws Exception {
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bart\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(1, responses.size());
      UserSearchResponse response = responses.get(0);

      assertEquals(bart.getId(), response.id());
      assertEquals("bart", response.username());
      assertEquals("Bart", response.name());
      assertTrue(response.isVerified());
      assertTrue(response.relationshipStatuses().isEmpty());
    }

    @Test
    public void searchUsers_excludesSuspendedUsers_fromResults() throws Exception {
      // Given: A suspended user with a username that would match the search
      User suspendedUser =
          User.builder()
              .username("suspendedbert")
              .email("suspended@test.com")
              .password("password123")
              .name("Suspended Bert")
              .isVerified(true)
              .emailVerified(true)
              .isSuspended(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      userRepository.save(suspendedUser);

      // When: Searching with query that would match the suspended user
      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/users")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          "{\"query\":\"bert\",\"excludeFriends\":false,\"excludeFollowed\":false,\"verifiedOnly\":false}")
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      // Then: Suspended user should NOT appear in results
      List<UserSearchResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(
          responses.stream().anyMatch(r -> r.username().equals("suspendedbert")),
          "Suspended users should be excluded from search results");
      assertTrue(
          responses.stream().anyMatch(r -> r.username().equals("bert")),
          "Non-suspended users with matching username should still appear");
    }

    private void createFollow(User follower, User followed) {
      FollowRelation relation =
          FollowRelation.builder()
              .id(new FollowRelationId(follower.getId(), followed.getId()))
              .follower(follower)
              .followed(followed)
              .since(LocalDateTime.now())
              .build();
      followRelationRepository.save(relation);
    }

    private void createFriendRequest(User sender, User receiver) {
      FriendRequest request =
          FriendRequest.builder()
              .id(new FriendRequestId(sender.getId(), receiver.getId()))
              .sender(sender)
              .receiver(receiver)
              .createdAt(LocalDateTime.now())
              .expiresAt(LocalDateTime.now().plusDays(30))
              .build();
      friendRequestRepository.save(request);
    }

    private void createFriendship(User user1, User user2) {
      FriendRelation friendship =
          FriendRelation.builder()
              .id(new FriendRelationId(user1.getId(), user2.getId()))
              .friend1(user1)
              .friend2(user2)
              .since(LocalDateTime.now())
              .build();
      friendRelationRepository.save(friendship);
    }

    private boolean hasStatus(
        UserSearchResponse response, RelationshipStatusItem.RelationshipStatus status) {
      return response.relationshipStatuses().stream().anyMatch(item -> item.status() == status);
    }
  }

  // ==================== POST SEARCH TESTS ====================

  @Nested
  @Transactional
  class PostSearchTests {

    private User currentUser;
    private User otherUser;
    private Topic travelTopic;
    private Topic foodTopic;
    private Topic techTopic;
    private Post post1;
    private Post post2;
    private Post post3;
    private Post post4;

    @BeforeEach
    void setUp() {
      currentUser =
          User.builder()
              .username("searchuser")
              .email("search@test.com")
              .password("password123")
              .name("Search User")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      currentUser = userRepository.save(currentUser);

      otherUser =
          User.builder()
              .username("postcreator")
              .email("creator@test.com")
              .password("password123")
              .name("Post Creator")
              .isVerified(true)
              .emailVerified(true)
              .birthdate(LocalDate.of(2000, 1, 1))
              .build();
      otherUser = userRepository.save(otherUser);

      travelTopic = createTopic("travel", "✈️");
      foodTopic = createTopic("food", "🍕");
      techTopic = createTopic("tech", "💻");

      post1 =
          createPost(
              "Beach vacation in Hawaii with sunny weather",
              true,
              generateEmbedding(0.9f),
              otherUser);
      assignTopic(post1, travelTopic);

      post2 =
          createPost("Summer beach party with friends", true, generateEmbedding(0.8f), otherUser);
      assignTopic(post2, travelTopic);

      post3 =
          createPost("Italian pasta and pizza dinner", true, generateEmbedding(0.3f), otherUser);
      assignTopic(post3, foodTopic);

      post4 =
          createPost(
              "Latest programming languages and frameworks",
              true,
              generateEmbedding(0.1f),
              otherUser);
      assignTopic(post4, techTopic);
    }

    private Topic createTopic(String name, String emoji) {
      Topic topic =
          Topic.builder()
              .name(name)
              .emoji(emoji)
              .embedding(generateEmbedding((float) Math.random()))
              .build();
      return topicRepository.save(topic);
    }

    private Post createPost(String caption, boolean isPublic, float[] embedding, User user) {
      Post post =
          Post.builder()
              .postId(UUID.randomUUID().toString())
              .user(user)
              .caption(caption)
              .isPublic(isPublic)
              .isRemoved(false)
              .embedding(embedding)
              .createdAt(LocalDateTime.now())
              .upvotes(0)
              .downvotes(0)
              .postTopics(new LinkedHashSet<>())
              //.postLabels(new LinkedHashSet<>())
              .comments(new LinkedHashSet<>())
              .build();
      return postRepository.save(post);
    }

    private void assignTopic(Post post, Topic topic) {
      PostTopicId id = new PostTopicId(post.getPostId(), topic.getTopicId());
      PostTopic postTopic = PostTopic.builder().id(id).post(post).topic(topic).build();
      entityManager.persist(postTopic);
      post.getPostTopics().add(postTopic);
    }

    private float[] generateEmbedding(float similarity) {
      float[] embedding = new float[EmbeddingModelConfig.MODEL_DIMENSIONS];
      Arrays.fill(embedding, similarity);
      return embedding;
    }

    private String buildSearchJson(
        String query, List<String> topicIds, int page, int postsPerPage) {
      StringBuilder json = new StringBuilder();
      json.append("{");
      if (query != null) {
        json.append("\"query\":\"").append(query).append("\",");
      } else {
        json.append("\"query\":null,");
      }
      if (topicIds != null && !topicIds.isEmpty()) {
        json.append("\"topicIds\":[");
        for (int i = 0; i < topicIds.size(); i++) {
          json.append("\"").append(topicIds.get(i)).append("\"");
          if (i < topicIds.size() - 1) json.append(",");
        }
        json.append("],");
      } else {
        json.append("\"topicIds\":null,");
      }
      json.append("\"page\":").append(page).append(",");
      json.append("\"postsPerPage\":").append(postsPerPage);
      json.append("}");
      return json.toString();
    }

    @Test
    public void testSearchEndpoint_withValidQuery_returns200AndResults() throws Exception {
      String requestJson = buildSearchJson("beach vacation", null, 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(responses.isEmpty());
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Beach vacation")),
          "Should find beach vacation post");
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Summer beach")),
          "Should find summer beach post");
    }

    @Test
    public void testSearchEndpoint_withValidTopics_returns200AndFilteredResults() throws Exception {
      String requestJson = buildSearchJson(null, List.of(foodTopic.getTopicId()), 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(responses.isEmpty());
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Italian pasta")),
          "Should find food post");
      assertFalse(
          responses.stream().anyMatch(r -> r.caption().contains("Beach")),
          "Should not find beach posts");
    }

    @Test
    public void testSearchEndpoint_withValidQueryAndTopics_returns200AndRankedFilteredResults()
        throws Exception {
      String requestJson = buildSearchJson("beach", List.of(travelTopic.getTopicId()), 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(responses.isEmpty());
      assertTrue(
          responses.stream()
              .allMatch(
                  r ->
                      r.caption().contains("Beach")
                          || r.caption().contains("vacation")
                          || r.caption().contains("beach")),
          "All results should contain beach-related terms");
    }

    @Test
    public void testSearchEndpoint_withNoMatches_returns200AndResultsOrEmpty() throws Exception {
      String requestJson = buildSearchJson("xyznonexistent12345", null, 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertNotNull(responses, "Should return a valid list");
    }

    @Test
    public void testSearchEndpoint_withQueryTooShort_returns400() throws Exception {
      String requestJson = buildSearchJson("ab", null, 0, 10);

      mockMvc
          .perform(
              post("/api/search/posts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson)
                  .with(user(currentUser)))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void testSearchEndpoint_withNoQueryAndNoTopics_returns400() throws Exception {
      String requestJson = buildSearchJson(null, null, 0, 10);

      mockMvc
          .perform(
              post("/api/search/posts")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestJson)
                  .with(user(currentUser)))
          .andExpect(status().isBadRequest());
    }

    @Test
    public void testSearchEndpoint_resultsAreOrderedByScoreDescending() throws Exception {
      String requestJson = buildSearchJson("beach vacation", null, 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      if (responses.size() >= 2) {
        assertTrue(
            responses.get(0).caption().contains("vacation")
                || responses.get(0).caption().contains("Beach vacation"),
            "First result should have better word match");
      }
    }

    @Test
    public void testSearchEndpoint_postsWithoutMatchingTopicAreExcluded() throws Exception {
      String requestJson = buildSearchJson(null, List.of(foodTopic.getTopicId()), 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(
          responses.stream().anyMatch(r -> r.caption().contains("Beach")),
          "Should not contain beach posts without food topic");
      assertFalse(
          responses.stream().anyMatch(r -> r.caption().contains("programming")),
          "Should not contain tech posts without food topic");
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Italian")),
          "Should contain food post");
    }

    @Test
    public void testSearchEndpoint_postsMustContainAtLeastOneSelectedTopic() throws Exception {
      assignTopic(post1, foodTopic);

      String requestJson = buildSearchJson(null, List.of(foodTopic.getTopicId()), 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertEquals(2, responses.size(), "Should find 2 posts with food topic");
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Italian pasta")),
          "Should find post3");
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Beach vacation")),
          "Should find post1 which has both topics");
    }

    @Test
    public void testSearchEndpoint_excludesRemovedPosts_fromResults() throws Exception {
      // Given: Create a removed post that would match the search
      Post removedPost = createPost("Removed beach post", true, generateEmbedding(0.9f), otherUser);
      removedPost.setIsRemoved(true);
      postRepository.save(removedPost);

      // When: Searching for "beach"
      String requestJson = buildSearchJson("beach", null, 0, 10);

      MvcResult result =
          mockMvc
              .perform(
                  post("/api/search/posts")
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(requestJson)
                      .with(user(currentUser)))
              .andExpect(status().isOk())
              .andReturn();

      // Then: Removed post should NOT appear in results
      List<PostPreviewResponse> responses =
          objectMapper.readValue(
              result.getResponse().getContentAsString(), new TypeReference<>() {});

      assertFalse(
          responses.stream().anyMatch(r -> r.caption().equals("Removed beach post")),
          "Removed posts should be excluded from search results");
      assertTrue(
          responses.stream().anyMatch(r -> r.caption().contains("Beach vacation")),
          "Non-removed posts should still appear");
    }
  }
}
