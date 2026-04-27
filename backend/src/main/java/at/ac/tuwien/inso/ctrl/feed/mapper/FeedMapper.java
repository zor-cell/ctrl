package at.ac.tuwien.inso.ctrl.feed.mapper;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FeedMapper {

    private final PostVoteRepository postVoteRepository;
    private final SourceVoteRepository sourceVoteRepository;

    public FeedPostResponse toFeedPostResponse(Post post, String userId) {
        String authorId   = post.isExternalPost() ? null                                      : post.getUser().getId().toString();
        String username   = post.isExternalPost() ? post.getExternalAuthor().getDisplayName() : post.getUser().getUsername();
        String profilePic = post.isExternalPost() ? post.getExternalAuthor().getAvatarUrl()   : post.getUser().getProfilePicture();

        // Current user's post-vote
        String postUserVote = null;
        if (userId != null) {
            postUserVote = postVoteRepository
                    .findById(new PostVoteId(post.getPostId(), userId))
                    .map(v -> v.getVoteType())
                    .orElse(null);
        }

        List<FeedPostResponse.LabelResponse> labels = post.getPostLabels().stream()
                .map(pl -> new FeedPostResponse.LabelResponse(
                        pl.getLabel().getName(),
                        pl.getLabel().getExplanation()
                ))
                .toList();

        // Source-Votes: sourceId + userVote pro SingleSource
        List<FeedPostResponse.SourceResponse> sources = post.getSource() != null
                ? post.getSource().getSingleSources().stream()
                .map(s -> {
                    String sourceUserVote = null;
                    if (userId != null) {
                        sourceUserVote = sourceVoteRepository
                                .findById(new SourceVoteId(post.getSource().getSourceId(), userId))
                                .map(v -> v.getVoteType())
                                .orElse(null);
                    }
                    return new FeedPostResponse.SourceResponse(
                            post.getSource().getSourceId(),
                            s.getUrl(),
                            post.getSource().getUpvotes(),
                            post.getSource().getDownvotes(),
                            sourceUserVote
                    );
                })
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