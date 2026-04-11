package at.ac.tuwien.inso.ctrl.post.mapper;

import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {
  public PostResponse toResponse(Post post) {
    String sourceId = post.getSource() != null ? post.getSource().getSourceId() : null;
    List<String> labelIds =
        post.getPostLabels().stream()
            .map(postLabel -> postLabel.getLabel().getLabelId())
            .sorted(Comparator.naturalOrder())
            .toList();
    List<String> topicIds =
        post.getPostTopics().stream()
            .map(postTopic -> postTopic.getTopic().getTopicId())
            .sorted(Comparator.naturalOrder())
            .toList();

    return new PostResponse(
        post.getPostId(),
        post.getUser().getId(),
        post.getUser().getUsername(),
        sourceId,
        post.getCaption(),
        post.getMediaRef(),
        post.isPublic(),
        post.isAiGenerated(),
        labelIds,
        topicIds,
        post.getUpvotes(),
        post.getDownvotes(),
        post.getCreatedAt());
  }
}
