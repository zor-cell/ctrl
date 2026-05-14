package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Label;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostLabel;
import at.ac.tuwien.inso.ctrl.post.entity.PostLabelId;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PostMapperUnitTest {

  @Mock private FileStorageService fileStorageService;

  @Test
  void toResponse_withPostContent_resolvesMediaUrlAndSortsNestedValues() {
    PostMapper postMapper = new PostMapper(fileStorageService);
    User user = createUser("marlene");
    Source source = createSource();
    Post post =
        Post.builder()
            .postId("post-1")
            .user(user)
            .source(source)
            .caption("Caption")
            .mediaRef("posts/media/file.png")
            .isPublic(true)
            .isAiGenerated(true)
            .upvotes(2)
            .downvotes(1)
            .createdAt(LocalDateTime.of(2026, 4, 25, 12, 0))
            .build();
    post.setPostLabels(createPostLabels(post));
    post.setPostTopics(createPostTopics(post));
    post.setComments(createComments(post, user));
    when(fileStorageService.resolveFileUrl("posts/media/file.png"))
        .thenReturn("http://localhost:3900/ctrl-test-media/posts/media/file.png");

    PostResponse response = postMapper.toResponse(post);

    assertEquals("post-1", response.postId());
    assertEquals(user.getId(), response.userId());
    assertEquals("marlene", response.username());
    assertEquals("source-1", response.sourceId());
    assertEquals("Caption", response.caption());
    assertEquals("posts/media/file.png", response.mediaRef());
    assertEquals("http://localhost:3900/ctrl-test-media/posts/media/file.png", response.mediaUrl());
    assertEquals(List.of("alpha", "zulu"), response.labelIds());
    assertEquals(List.of("topic-a", "topic-z"), response.topicIds());
    assertEquals("older comment", response.comments().get(0).content());
    assertEquals("newer comment", response.comments().get(1).content());
    assertEquals("Alpha source", response.source().singleSources().get(0).title());
    assertEquals("Zulu source", response.source().singleSources().get(1).title());
    verify(fileStorageService).resolveFileUrl("posts/media/file.png");
  }

  private User createUser(String username) {
    User user = new User();
    user.setId(UUID.randomUUID());
    user.setUsername(username);
    return user;
  }

  private Source createSource() {
    Source source =
        Source.builder()
            .sourceId("source-1")
            .upvotes(0)
            .downvotes(0)
            .createdAt(LocalDateTime.of(2026, 4, 25, 10, 0))
            .build();
    source.setSingleSources(
        List.of(
            SingleSource.builder()
                .singleSourceId("source-z")
                .source(source)
                .title("Zulu source")
                .author("Author Z")
                .url("https://example.com/z")
                .date(LocalDate.of(2026, 4, 25))
                .build(),
            SingleSource.builder()
                .singleSourceId("source-a")
                .source(source)
                .title("Alpha source")
                .author("Author A")
                .url("https://example.com/a")
                .date(LocalDate.of(2026, 4, 24))
                .build()));
    return source;
  }

  private LinkedHashSet<PostLabel> createPostLabels(Post post) {
    Label zulu = Label.builder().labelId("zulu").name("Zulu").emoji("Z").build();
    Label alpha = Label.builder().labelId("alpha").name("Alpha").emoji("A").build();
    return new LinkedHashSet<>(
        List.of(
            PostLabel.builder()
                .id(new PostLabelId("post-1", "zulu"))
                .post(post)
                .label(zulu)
                .build(),
            PostLabel.builder()
                .id(new PostLabelId("post-1", "alpha"))
                .post(post)
                .label(alpha)
                .build()));
  }

  private LinkedHashSet<PostTopic> createPostTopics(Post post) {
    Topic topicZ = Topic.builder().topicId("topic-z").name("Zulu").build();
    Topic topicA = Topic.builder().topicId("topic-a").name("Alpha").build();
    return new LinkedHashSet<>(
        List.of(
            PostTopic.builder()
                .id(new PostTopicId("post-1", "topic-z"))
                .post(post)
                .topic(topicZ)
                .build(),
            PostTopic.builder()
                .id(new PostTopicId("post-1", "topic-a"))
                .post(post)
                .topic(topicA)
                .build()));
  }

  private LinkedHashSet<Comment> createComments(Post post, User user) {
    return new LinkedHashSet<>(
        List.of(
            Comment.builder()
                .commentId("comment-new")
                .post(post)
                .user(user)
                .content("newer comment")
                .createdAt(LocalDateTime.of(2026, 4, 25, 12, 0))
                .build(),
            Comment.builder()
                .commentId("comment-old")
                .post(post)
                .user(user)
                .content("older comment")
                .createdAt(LocalDateTime.of(2026, 4, 25, 10, 0))
                .build()));
  }
}
