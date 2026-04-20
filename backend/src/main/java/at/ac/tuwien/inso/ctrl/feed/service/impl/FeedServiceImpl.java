package at.ac.tuwien.inso.ctrl.feed.service.impl;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final PostRepository postRepository;

    @Override
    @Transactional(readOnly = true)
    public List<FeedPostResponse> getFeed(int page, int postsPerPage) {
        var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").descending());
        return postRepository.findAll(pageable)
                .stream()
                .map(this::toFeedPostResponse)
                .toList();
    }

    private FeedPostResponse toFeedPostResponse(Post post) {
        String userId     = post.isExternalPost() ? null                                     : post.getUser().getId().toString();
        String username   = post.isExternalPost() ? post.getExternalAuthor().getDisplayName() : post.getUser().getUsername();
        String profilePic = post.isExternalPost() ? post.getExternalAuthor().getAvatarUrl()   : post.getUser().getProfilePicture();

        List<FeedPostResponse.LabelResponse> labels = post.getPostLabels().stream()
                .map(pl -> new FeedPostResponse.LabelResponse(
                        pl.getLabel().getName(),
                        pl.getLabel().getExplanation()
                ))
                .toList();

        List<FeedPostResponse.SourceResponse> sources = post.getSource() != null
                ? post.getSource().getSingleSources().stream()
                .map(s -> new FeedPostResponse.SourceResponse(s.getUrl(), 0, 0))
                .toList()
                : List.of();

        List<FeedPostResponse.CommentResponse> comments = post.getComments().stream()
                .map(this::toCommentResponse)
                .toList();

        List<String> topicIds = post.getPostTopics().stream()
                .map(pt -> pt.getTopic().getName())
                .toList();

        return new FeedPostResponse(
                post.getPostId(),
                userId,
                username,
                profilePic,
                post.getCaption(),
                post.getMediaRef(),
                post.isPublic(),
                post.getUpvotes(),
                post.getDownvotes(),
                post.getCreatedAt(),
                topicIds,
                labels,
                sources,
                comments
        );
    }

    private FeedPostResponse.CommentResponse toCommentResponse(Comment comment) {
        return new FeedPostResponse.CommentResponse(
                comment.getCommentId(),
                comment.getUser().getId().toString(),
                comment.getUser().getUsername(),
                comment.getUser().getProfilePicture(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null
        );
    }
}