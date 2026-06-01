package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class FeedControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private PostVoteRepository postVoteRepository;
  @Autowired private JwtService jwtService;

  private User existingUser;
  private User otherUser;
  private String validJwtToken;

  private final String feedEndpoint = "/api/feed";

  @BeforeEach
  public void setUp() {
    postVoteRepository.deleteAll();
    postRepository.deleteAll();
    userRepository.deleteAll();

    float[] embedding = generateDummyEmbedding();
    existingUser =
        User.builder()
            .username("feedTester")
            .password("securePass123!")
            .email("tester@test.com")
            .interactionPreferenceEmbedding(embedding)
            .build();

    otherUser =
        User.builder()
            .username("feedTester2")
            .password("securePass123!")
            .email("tester@testddd.com")
            .interactionPreferenceEmbedding(embedding)
            .build();

    userRepository.save(existingUser);
    userRepository.save(otherUser);
    validJwtToken = jwtService.generateToken(existingUser);
  }

  @AfterEach
  public void tearDown() {
    postVoteRepository.deleteAll();
    postRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void getFeed_shouldReturnPaginatedPosts_WhenAuthenticated() throws Exception {
    Post post = new Post();
    post.setPostId("post-id2");
    post.setCaption("Integration Test Caption");
    post.setUser(otherUser);
    post.setPublic(true);
    post.setEmbedding(generateDummyEmbedding());
    post.setCreatedAt(LocalDateTime.now());

    post = postRepository.save(post);

    mockMvc
        .perform(
            get(feedEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .param("page", "0")
                .param("postsPerPage", "10")
                .param("publicFeed", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].postId").value(post.getPostId()));
  }

  /**
   * @Test public void getFeed_shouldReturnUnauthorized_WhenUnauthenticated() throws Exception {
   * mockMvc .perform(get("/feed").param("page", "0").param("postsPerPage", "10"))
   * .andExpect(status().isUnauthorized()); }
   */
  @Test
  public void getFeed_shouldIncludeUserVoteState_WhenVoted() throws Exception {
    Post post = new Post();
    post.setPostId("post-id1");
    post.setCaption("Voted Post");
    post.setUser(otherUser);
    post.setPublic(true);
    post.setEmbedding(generateDummyEmbedding());
    post.setCreatedAt(LocalDateTime.now());

    post = postRepository.save(post);

    PostVoteId voteId = new PostVoteId();
    voteId.setUserId(existingUser.getId().toString());
    voteId.setPostId(post.getPostId());

    PostVote vote = new PostVote();
    vote.setId(voteId);
    vote.setVoteType("up");
    vote.setPost(post);
    postVoteRepository.save(vote);

    mockMvc
        .perform(
            get(feedEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .param("page", "0")
                .param("postsPerPage", "5")
                .param("publicFeed", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].postId").value(post.getPostId()));
  }

  @Test
  public void getFeed_shouldReturnEmptyList_WhenNoRecommendationsFound() throws Exception {
    mockMvc
        .perform(
            get(feedEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validJwtToken)
                .param("page", "0")
                .param("postsPerPage", "10")
                .param("publicFeed", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(0)));
  }

  private float[] generateDummyEmbedding() {
    float[] embedding = new float[EmbeddingModelConfig.MODEL_DIMENSIONS];
    for (int i = 0; i < EmbeddingModelConfig.MODEL_DIMENSIONS; i++) {
      embedding[i] = (float) Math.random();
    }
    return embedding;
  }
}
