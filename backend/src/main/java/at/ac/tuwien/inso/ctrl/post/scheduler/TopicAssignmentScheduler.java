package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Periodically assigns 1–3 topics to posts that have no topic yet.
 * ┌──────────────────────────────────────────────────────────┐
 * │             TopicAssignmentScheduler                     │
 * │                                                          │
 * │  Every 30 minutes:                                       │
 * │                                                          │
 * │  1. Find posts without topics (batches of BATCH_SIZE)    │
 * │                 ↓                                        │
 * │  2. For each post, query pg vector for the 3 closest      │
 * │     topic embeddings via cosine distance (<=>)           │
 * │                 ↓                                        │
 * │  3. Persist PostTopic join entries                       │
 * │                 ↓                                        │
 * │  4. Repeat until no untagged posts remain                │
 * └──────────────────────────────────────────────────────────┘
 * Requires PostRepository to expose:
 *   Page<Post> findByPostTopicsEmpty(Pageable pageable)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TopicAssignmentScheduler {

  /** How many posts to process per scheduler-run. Prevents long-running transactions. */
  private static final int BATCH_SIZE = 200;

  /** How many topics to assign per post (1–3 recommended). */
  private static final int TOPICS_PER_POST = 3;

  private final PostRepository postRepository;
  private final TopicRepository topicRepository;

  //@Scheduled(fixedRateString = "PT30M")
  @Scheduled(fixedDelay = 60000) // Alle 60 Sekunden statt alle 30 Min
  @Transactional
  public void assignTopics() {
    log.info("TopicAssignmentScheduler started");

    long total = postRepository.countPostsWithoutTopics();
    if (total == 0) {
      log.info("TopicAssignmentScheduler finished — all posts already have topics");
      return;
    }

    log.info("{} posts without topics found, processing up to {} this run", total, BATCH_SIZE);

    List<Post> posts = postRepository.findPostsWithoutTopics(BATCH_SIZE, 0);

    int assigned = 0;
    int skipped  = 0;

    for (Post post : posts) {
      if (post.getEmbedding() == null) {
        log.debug("Skipping post {} — no embedding", post.getPostId());
        skipped++;
        continue;
      }
      if (assignTopicsToPost(post.getPostId())) assigned++;
    }

    log.info("TopicAssignmentScheduler finished — assigned topics to {}/{} posts ({} skipped, no embedding)",
        assigned, posts.size(), skipped);
  }

  public boolean assignTopicsToPost(String postId) {
    try {
      // Reload post -> It is now "attached" to the current session
      Post post = postRepository.findById(postId)
              .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

      List<Topic> topics = topicRepository.findMatchingTopicsForEmbedding(
              post.getEmbedding(), TOPICS_PER_POST);

      if (topics.isEmpty()) {
        log.warn("No matching topics found for post {}", post.getPostId());
        return false;
      }

      for (Topic topic : topics) {
        PostTopicId id = new PostTopicId(post.getPostId(), topic.getTopicId());
        PostTopic postTopic = PostTopic.builder()
                .id(id)
                .post(post)
                .topic(topic)
                .build();
        post.getPostTopics().add(postTopic);
      }

      postRepository.save(post);
      log.debug("Post {} → topics: {}", post.getPostId(),
              topics.stream().map(Topic::getName).toList());

      return true;
    } catch (Exception e) {
      log.error("Failed to assign topics to post {}: {}", postId, e.getMessage());
      return false;
    }
  }
}
