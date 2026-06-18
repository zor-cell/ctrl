package at.ac.tuwien.inso.ctrl.label.service;

import at.ac.tuwien.inso.ctrl.label.entity.LabelContext;
import at.ac.tuwien.inso.ctrl.label.entity.PostLabelType;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FollowRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabelingService {

  private final PostRepository postRepository;
  private final FollowRelationRepository followRelationRepository;
  private final FriendRelationRepository friendRelationRepository;

  public LabelContext buildContext(User user) {
    Integer p95 = postRepository.findUpvotePercentile95();
    return new LabelContext(
        user.getId(),
        followRelationRepository.findFollowedIds(user.getId()),
        friendRelationRepository.findFriendIds(user),
        followRelationRepository.findSecondDegreeIds(user.getId()),
        p95 != null ? p95 : 0,
        LocalDateTime.now());
  }

  public Set<PostLabelType> computeLabels(Post post, LabelContext ctx, boolean postReportedByUser) {
    Set<PostLabelType> labels = new HashSet<>();

    if (postReportedByUser) labels.add(PostLabelType.POST_REPORTED);

    if (post.getCreatedAt() != null) {
      if (post.getCreatedAt().isAfter(ctx.now().minusHours(6))) labels.add(PostLabelType.RECENT);
      if (post.getCreatedAt().isBefore(ctx.now().minusMonths(6)))
        labels.add(PostLabelType.OLD_POST);
    }

    // Post properties labels
    if (post.isAiGenerated()) labels.add(PostLabelType.AI_GENERATED);

    if (post.isExternalPost()) labels.add(PostLabelType.MASTODON);

    if (post.getUpvotes() != null
        && post.getUpvotes() >= 5) // && post.getUpvotes() >= ctx.upvoteThreshold())
    labels.add(PostLabelType.POPULAR);

    if (post.getComments() != null && post.getComments().size() > 5)
      labels.add(PostLabelType.HIGHLY_DISCUSSED);

    // Source Trust via Wilson Score
    if (post.getSource() != null) {
      int up = post.getSource().getUpvotes() != null ? post.getSource().getUpvotes() : 0;
      int down = post.getSource().getDownvotes() != null ? post.getSource().getDownvotes() : 0;
      int total = up + down;
      if (total > 5) {
        double score = wilsonScore(up, total);
        if (score > 0.7) labels.add(PostLabelType.TRUSTED_SOURCE);
        if (score < 0.3) labels.add(PostLabelType.DISPUTED_SOURCE);
      }
    }

    // Extended Network label
    if (post.getUser() != null) {
      UUID creatorId = post.getUser().getId();
      boolean isOwnPost = creatorId.equals(ctx.viewerId());
      boolean isDirectFollow = ctx.followedIds().contains(creatorId);
      boolean isDirectFriend = ctx.friendIds().contains(creatorId);
      boolean isSecondDegree = ctx.secondDegreeIds().contains(creatorId);

      if (!isOwnPost && !isDirectFollow && !isDirectFriend && isSecondDegree)
        labels.add(PostLabelType.EXTENDED_NETWORK);
    }

    return labels;
  }

  private double wilsonScore(int upvotes, int total) {
    double p = (double) upvotes / total;
    double z = 1.96;
    double denom = 1 + z * z / total;
    double centre = p + z * z / (2.0 * total);
    double spread = z * Math.sqrt(p * (1 - p) / total + z * z / (4.0 * total * total));
    return (centre - spread) / denom;
  }
}
