package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Label;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostLabel;
import at.ac.tuwien.inso.ctrl.post.entity.PostLabelId;
import at.ac.tuwien.inso.ctrl.post.mapper.PostMapper;
import at.ac.tuwien.inso.ctrl.post.repository.LabelRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
  private static final int INITIAL_VOTE_COUNT = 0;

  private final PostRepository postRepository;
  private final LabelRepository labelRepository;
  private final UserRepository userRepository;
  private final SourceService sourceService;
  private final PostMapper postMapper;

  @Override
  @Transactional
  public PostResponse createPost(CreatePostRequest request, String username) {
    var user =
        userRepository
            .findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

    Source source = sourceService.createSource(request.source());

    var post =
        Post.builder()
            .postId(UUID.randomUUID().toString())
            .user(user)
            .source(source)
            .caption(request.caption())
            .mediaRef(request.mediaRef())
            .isPublic(resolveIsPublic(request, user.isVerified()))
            .upvotes(INITIAL_VOTE_COUNT)
            .downvotes(INITIAL_VOTE_COUNT)
            .createdAt(LocalDateTime.now())
            .build();
    post.setPostLabels(buildPostLabels(post, request.labelIds()));

    return postMapper.toResponse(postRepository.save(post));
  }

  private boolean resolveIsPublic(CreatePostRequest request, boolean isVerified) {
    return isVerified && Boolean.TRUE.equals(request.isPublic());
  }

  private Set<PostLabel> buildPostLabels(Post post, List<String> labelIds) {
    if (labelIds == null || labelIds.isEmpty()) {
      return new LinkedHashSet<>();
    }

    List<String> uniqueLabelIds = labelIds.stream().distinct().toList();
    List<Label> labels = labelRepository.findAllById(uniqueLabelIds);

    if (labels.size() != uniqueLabelIds.size()) {
      Set<String> foundLabelIds =
          labels.stream().map(Label::getLabelId).collect(Collectors.toSet());
      String missingLabelIds =
          uniqueLabelIds.stream()
              .filter(labelId -> !foundLabelIds.contains(labelId))
              .sorted()
              .collect(Collectors.joining(", "));
      throw new ResourceNotFoundException("Labels not found: " + missingLabelIds);
    }

    return labels.stream()
        .map(label -> createPostLabel(post, label))
        .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  private PostLabel createPostLabel(Post post, Label label) {
    return PostLabel.builder()
        .id(new PostLabelId(post.getPostId(), label.getLabelId()))
        .post(post)
        .label(label)
        .build();
  }
}
