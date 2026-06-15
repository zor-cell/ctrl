package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopic;
import at.ac.tuwien.inso.ctrl.post.entity.PostTopicId;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
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
   * Maximum number of topics to assign per post. The actual number may be lower if fewer topics
   * pass the similarity threshold.
   */
  private static final int MAX_TOPICS_PER_POST = 3;

  /**
   * Minimum number of topics required to consider an assignment successful. If fewer topics than
   * this pass the similarity threshold, the post is left untagged (BELOW_THRESHOLD). A value of 1
   * means any single matching topic is sufficient.
   */
  private static final int MIN_TOPICS_PER_POST = 1;

  /**
   * Cosine distance cutoff (pgvector <=> operator, range 0-2). Only topics whose distance to the
   * post embedding is strictly below this value are assigned. This prevents irrelevant topics from
   * being force-assigned just because they are the "least bad" match.
   */
  private static final double SIMILARITY_THRESHOLD = 0.75;

  private final PostRepository postRepository;
  private final TopicRepository topicRepository;

  @Scheduled(fixedDelay = 1000_000) // every 10 minutes
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

      // Step 1: always guarantee MIN_TOPICS_PER_POST assignments (best matches, no threshold).
      List<Topic> guaranteed =
          topicRepository.findTopNTopicsForEmbedding(post.getEmbedding(), MIN_TOPICS_PER_POST);

      // Step 2: fetch additional topics that pass the similarity threshold, up to MAX.
      List<Topic> thresholdBased =
          topicRepository.findMatchingTopicsForEmbedding(
              post.getEmbedding(), MAX_TOPICS_PER_POST, SIMILARITY_THRESHOLD);

      // Merge: guaranteed first, then threshold-based extras (deduped by topicId).
      java.util.LinkedHashMap<String, Topic> merged = new java.util.LinkedHashMap<>();
      for (Topic t : guaranteed) merged.put(t.getTopicId(), t);
      for (Topic t : thresholdBased) merged.put(t.getTopicId(), t);
      List<Topic> topics = new java.util.ArrayList<>(merged.values());

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
