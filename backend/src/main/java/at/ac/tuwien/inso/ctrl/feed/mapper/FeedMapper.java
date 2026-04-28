package at.ac.tuwien.inso.ctrl.feed.mapper;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FeedMapper {


    public FeedPostResponse toFeedPostResponse(Post post,
                                               Map<String, String> postVotes,
                                               Map<String, String> sourceVotes) {
        String authorId   = post.isExternalPost() ? null                                      : post.getUser().getId().toString();
        String username   = post.isExternalPost() ? post.getExternalAuthor().getDisplayName() : post.getUser().getUsername();
        String profilePic = post.isExternalPost() ? post.getExternalAuthor().getAvatarUrl()   : post.getUser().getProfilePicture();

        String postUserVote = postVotes.get(post.getPostId());

        List<FeedPostResponse.LabelResponse> labels = post.getPostLabels().stream()
                .map(pl -> new FeedPostResponse.LabelResponse(
                        pl.getLabel().getName(),
                        pl.getLabel().getExplanation()
                ))
                .toList();

        List<FeedPostResponse.SourceResponse> sources = post.getSource() != null
                ? post.getSource().getSingleSources().stream()
                .map(s -> new FeedPostResponse.SourceResponse(
                        post.getSource().getSourceId(),
                        s.getUrl(),
                        post.getSource().getUpvotes(),
                        post.getSource().getDownvotes(),
                        sourceVotes.get(post.getSource().getSourceId())
                ))
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
                authorId,
                username,
                profilePic,
                post.getCaption(),
                post.getMediaRef(),
                post.isPublic(),
                post.getUpvotes(),
                post.getDownvotes(),
                postUserVote,
                post.getCreatedAt(),
                topicIds,
                labels,
                sources,
                comments
        );
    }

    public FeedPostResponse.CommentResponse toCommentResponse(Comment comment) {
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