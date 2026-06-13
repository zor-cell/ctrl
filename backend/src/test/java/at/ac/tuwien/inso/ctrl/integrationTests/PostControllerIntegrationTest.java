package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.exception.GlobalExceptionHandler;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.controller.PostController;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class PostControllerIntegrationTest {

  private MockMvc mockMvc;

  @Mock private PostService postService;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new PostController(postService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // Gültiger POST /posts Request -> 201 Created + Post JSON

  @Test
  void createPost_ShouldReturnCreatedPost_WhenRequestIsValid() throws Exception {
    User user = createUser("marlene");
    authenticate(user);
    PostResponse response = createPostResponse(user, "post-1", "A valid caption");

    when(postService.createPost(any(CreatePostRequest.class), eq("marlene"))).thenReturn(response);

    mockMvc
        .perform(
            post("/posts")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caption": "A valid caption",
                      "mediaRef": "posts/media/file.png",
                      "isPublic": true,
                      "isAiGenerated": false,
                      "topicIds": ["topic-1"],
                      "source": null
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.postId").value("post-1"))
        .andExpect(jsonPath("$.username").value("marlene"))
        .andExpect(jsonPath("$.caption").value("A valid caption"))
        .andExpect(jsonPath("$.mediaRef").value("posts/media/file.png"))
        .andExpect(jsonPath("$.isPublic").value(true))
        .andExpect(jsonPath("$.topicIds[0]").value("topic-1"));

    verify(postService).createPost(any(CreatePostRequest.class), eq("marlene"));
  }

  @Test
  void createPost_ShouldReturnBadRequest_WhenCaptionIsBlank() throws Exception {
    User user = createUser("marlene");
    authenticate(user);

    mockMvc
        .perform(
            post("/posts")
                .principal(authenticationFor(user))
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

    verify(postService, never()).createPost(any(), any());
  }

  @Test
  void createPost_ShouldReturnNotFound_WhenServiceRejectsUnknownTopic() throws Exception {
    User user = createUser("marlene");
    authenticate(user);

    when(postService.createPost(any(CreatePostRequest.class), eq("marlene")))
        .thenThrow(new ResourceNotFoundException("Topics not found: missing"));

    mockMvc
        .perform(
            post("/posts")
                .principal(authenticationFor(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "caption": "A valid caption",
                      "mediaRef": null,
                      "isPublic": false,
                      "isAiGenerated": false,
                      "topicIds": ["missing"],
                      "source": null
                    }
                    """))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Topics not found: missing"));
  }

  @Test
  void getMyPosts_ShouldReturnPostsForAuthenticatedUser() throws Exception {
    User user = createUser("marlene");
    authenticate(user);
    PostResponse response = createPostResponse(user, "post-1", "My post");

    when(postService.getPostsForUser("marlene")).thenReturn(List.of(response));

    mockMvc
        .perform(get("/posts/me").principal(authenticationFor(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].postId").value("post-1"))
        .andExpect(jsonPath("$[0].caption").value("My post"));

    verify(postService).getPostsForUser("marlene");
  }

  @Test
  void getProfilePosts_ShouldReturnPostsForRequestedUser() throws Exception {
    User user = createUser("marlene");
    authenticate(user);
    PostResponse response = createPostResponse(user, "post-1", "Profile post");

    when(postService.getProfilePostsForUser("marlene", user)).thenReturn(List.of(response));

    mockMvc
        .perform(get("/posts/user/marlene").principal(authenticationFor(user)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].postId").value("post-1"))
        .andExpect(jsonPath("$[0].caption").value("Profile post"));

    verify(postService).getProfilePostsForUser("marlene", user);
  }

  @Test
  void getProfilePosts_ShouldReturnEmptyList_WhenUserHasNoPosts() throws Exception {
    User user = createUser("marlene");
    authenticate(user);

    when(postService.getProfilePostsForUser("marlene", user)).thenReturn(List.of());

    mockMvc
        .perform(get("/posts/user/marlene").principal(authenticationFor(user)))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));

    verify(postService).getProfilePostsForUser("marlene", user);
  }

  @Test
  void deletePost_ShouldReturnNoContent_WhenPostBelongsToUser() throws Exception {
    User user = createUser("marlene");
    authenticate(user);

    mockMvc
        .perform(delete("/posts/post-1").principal(authenticationFor(user)))
        .andExpect(status().isNoContent());

    verify(postService).deletePost("post-1", "marlene");
  }

  @Test
  void deletePost_ShouldReturnNotFound_WhenPostDoesNotBelongToUser() throws Exception {
    User user = createUser("marlene");
    authenticate(user);
    doThrow(new ResourceNotFoundException("Post not found"))
        .when(postService)
        .deletePost("post-1", "marlene");

    mockMvc
        .perform(delete("/posts/post-1").principal(authenticationFor(user)))
        .andExpect(status().isNotFound())
        .andExpect(content().string("Post not found"));

    verify(postService).deletePost("post-1", "marlene");
  }

  private User createUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private UsernamePasswordAuthenticationToken authenticationFor(User user) {
    return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
  }

  private void authenticate(User user) {
    SecurityContextHolder.getContext().setAuthentication(authenticationFor(user));
  }

  private PostResponse createPostResponse(User user, String postId, String caption) {
    return new PostResponse(
        postId,
        user.getId(),
        user.getUsername(),
        false,
        null,
        null,
        caption,
        "posts/media/file.png",
        true,
        false,
        List.of(),
        List.of("topic-1"),
        List.of(),
        0,
        0,
        LocalDateTime.of(2026, 4, 25, 12, 0));
  }
}
