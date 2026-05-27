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

@Slf4j
@Component
@RequiredArgsConstructor
public class TopicAssignmentScheduler {

  /** How many posts to process per scheduler run. Prevents long-running transactions. */
  private static final int BATCH_SIZE = 200;

  /**
   * Maximum number of topics to assign per post.
   * The actual number may be lower if fewer topics pass the similarity threshold.
   */
  private static final int MAX_TOPICS_PER_POST = 3;

  /**
   * Cosine distance cutoff (pgvector <=> operator, range 0-2).
   * Only topics whose distance to the post embedding is strictly below this value
   * are assigned. This prevents irrelevant topics from being force-assigned just
   * because they are the "least bad" match.
   * Tuning guide:
   *   0.25 = tight, high precision, may leave some posts with 0 topics
   *   0.35 = recommended starting point (cosine similarity >= 0.65)
   *   0.45 = looser, higher recall, some false positives
   * Raise if too many posts stay untagged after a scheduler run.
   * Lower if assigned topics feel inaccurate.
   */
  private static final double SIMILARITY_THRESHOLD = 0.8;

  private final PostRepository postRepository;
  private final TopicRepository topicRepository;

  @Scheduled(fixedDelay = 300_000) // every 5 minutes
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
    int skipped = 0;
    int belowThreshold = 0;

    for (Post post : posts) {
      if (post.getEmbedding() == null) {
        log.debug("Skipping post {} — no embedding", post.getPostId());
        skipped++;
        continue;
      }

      AssignmentResult result = assignTopicsToPost(post.getPostId());
      switch (result) {
        case SUCCESS -> assigned++;
        case BELOW_THRESHOLD -> belowThreshold++;
        default -> {} // FAILED — already logged inside assignTopicsToPost
      }
    }

    log.info(
            "TopicAssignmentScheduler finished — assigned: {}, below threshold: {}, skipped (no embedding): {}, failed: {}",
            assigned,
            belowThreshold,
            skipped,
            posts.size() - assigned - belowThreshold - skipped);
  }

  public AssignmentResult assignTopicsToPost(String postId) {
    try {
      Post post =
              postRepository
                      .findById(postId)
                      .orElseThrow(() -> new RuntimeException("Post not found: " + postId));

      List<Topic> topics =
              topicRepository.findMatchingTopicsForEmbedding(
                      post.getEmbedding(), MAX_TOPICS_PER_POST, SIMILARITY_THRESHOLD);

      if (topics.isEmpty()) {
        // No topic passed the threshold — post stays untagged for now.
        // This is intentional: a bad assignment is worse than no assignment.
        log.debug(
                "Post {} — no topics above similarity threshold ({}) found",
                post.getPostId(),
                SIMILARITY_THRESHOLD);
        return AssignmentResult.BELOW_THRESHOLD;
      }

      for (Topic topic : topics) {
        PostTopicId id = new PostTopicId(post.getPostId(), topic.getTopicId());
        PostTopic postTopic = PostTopic.builder().id(id).post(post).topic(topic).build();
        post.getPostTopics().add(postTopic);
      }

      postRepository.save(post);
      log.debug(
              "Post {} -> {} topic(s): {}",
              post.getPostId(),
              topics.size(),
              topics.stream().map(Topic::getName).toList());

      return AssignmentResult.SUCCESS;

    } catch (Exception e) {
      log.error("Failed to assign topics to post {}: {}", postId, e.getMessage());
      return AssignmentResult.FAILED;
    }
  }

  public enum AssignmentResult {
    /** At least one topic above the similarity threshold was assigned. */
    SUCCESS,
    /** No topic passed the similarity threshold — post intentionally left untagged. */
    BELOW_THRESHOLD,
    /** An unexpected exception occurred. */
    FAILED
  }
}