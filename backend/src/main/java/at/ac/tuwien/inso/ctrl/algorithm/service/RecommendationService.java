package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostScoreProjection;
import at.ac.tuwien.inso.ctrl.post.entity.RecommendedPost;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationService {
  private final AlgorithmService algorithmService;
  private final PostRepository postRepository;

  @Transactional
  public List<RecommendedPost> getRecommendedPosts(
      User user, int page, int size, boolean publicFeed) {
    var config = algorithmService.getOrCreateConfig(user);

    float[] interactionPreference = user.getInteractionPreferenceEmbedding();
    float[] topicPreference = config.getTopicPreferenceEmbedding();

    List<String> topics = config.getTopics().stream().map(Topic::getTopicId).toList();

    // fetch candidates according to which feed is requested
    List<String> candidatePosts;
    if (publicFeed) {
      candidatePosts =
          postRepository.getPublicPostCandidatePool(
              user.getId(), interactionPreference, topicPreference, 50);
    } else {
      candidatePosts =
          postRepository.getPrivatePostCandidatePool(
              user.getId(), interactionPreference, topicPreference, 50, false);

      // if no new posts are available, fetch the old ones
      if (candidatePosts.isEmpty()) {
        candidatePosts =
            postRepository.getPrivatePostCandidatePool(
                user.getId(), interactionPreference, topicPreference, 50, true);
      }
    }

    List<PostScoreProjection> scoredProjections =
        postRepository.getRankedPosts(
            candidatePosts,
            user.getId(),
            config.getWeightInteraction() / 100.0,
            interactionPreference,
            config.getWeightTopic() / 100.0,
            topicPreference,
            topics,
            config.getWeightRecency() / 100.0,
            config.getWeightPopularity() / 100.0,
            config.getWeightSourceQuality() / 100.0,
            config.getWeightExploration() / 100.0,
            size,
            page * size);
    if (scoredProjections.isEmpty()) {
      return List.of();
    }

    // fetch all posts from the db and create lookup table for retrieval
    List<String> postIds = scoredProjections.stream().map(PostScoreProjection::getPostId).toList();
    List<Post> fullPosts = postRepository.findAllById(postIds);
    Map<String, Post> postMap =
        fullPosts.stream().collect(Collectors.toMap(Post::getPostId, post -> post));

    return scoredProjections.stream()
        .map(
            score -> {
              Post post = postMap.get(score.getPostId());
              PostExplanation explanation =
                  new PostExplanation(
                      score.getScoreInteraction(),
                      score.getScoreTopic(),
                      score.getScoreRecency(),
                      score.getScoreTrending(),
                      score.getScoreSource(),
                      score.getScoreExploration(),
                      score.getTotalScore());

              return new RecommendedPost(post, explanation);
            })
        .toList();
  }
}
