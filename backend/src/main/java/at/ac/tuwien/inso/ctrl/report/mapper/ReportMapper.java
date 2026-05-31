package at.ac.tuwien.inso.ctrl.report.mapper;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.post.dto.response.SingleSourceResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.SourceResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedSourceResponse;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportMapper {
  private final SourceRepository sourceRepository;
  private final PostRepository postRepository;
  private final FeedMapper feedMapper;

  public ReportedSourceResponse toReportedSourceResponse(Report report) {
    Source source =
        sourceRepository
            .findById(report.getContentId())
            .orElseThrow(
                () -> new EntityNotFoundException("Source not found: " + report.getContentId()));

    SourceResponse sourceResponse = toSourceResponse(source);

    return new ReportedSourceResponse(
        // Report Info
        report.getReportId(),
        report.getReason(),
        report.getDescription(),
        report.getFromUser().getUsername(),
        report.getUsernameReported() != null
            ? report.getUsernameReported().getUsername()
            : getUserBySource(source),
        report.getContentType(),
        report.getStatus(),
        report.getCreatedAt(),
        sourceResponse);
  }

  public ReportedPostResponse toReportedPostResponse(
      Report report, Post post, Map<String, String> postVotes, Map<String, String> sourceVotes) {
    // Use the same mapping logic as FeedService
    FeedPostResponse feedPostResponse = feedMapper.toFeedPostResponse(post, postVotes, sourceVotes);

    return new ReportedPostResponse(
        // Report Info
        report.getReportId(),
        report.getReason(),
        report.getDescription(),
        report.getFromUser().getUsername(),
        report.getUsernameReported() != null
            ? report.getUsernameReported().getUsername()
            : feedPostResponse.username(),
        report.getContentType(),
        report.getStatus(),
        report.getCreatedAt(),

        // Post Info
        feedPostResponse.postId(),
        feedPostResponse.userId(),
        feedPostResponse.username(),
        feedPostResponse.userProfilePic(),
        feedPostResponse.caption(),
        feedPostResponse.mediaRef(),
        feedPostResponse.isPublic(),
        feedPostResponse.upvotes(),
        feedPostResponse.downvotes(),
        feedPostResponse.userVote(),
        feedPostResponse.createdAt(),
        feedPostResponse.topicIds(),
        feedPostResponse.labelIds(),
        feedPostResponse.sourceIds(),
        feedPostResponse.comments());
  }

  private String getUserBySource(Source source) {
    Post post =
        postRepository
            .findBySource(source)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        "Post not found for source: " + source.getSourceId()));
    return post.getExternalAuthor().getDisplayName();
  }

  private SourceResponse toSourceResponse(Source source) {
    if (source == null) {
      throw new EntityNotFoundException("Source not found");
    }

    return new SourceResponse(
        source.getSourceId(),
        source.getUpvotes(),
        source.getDownvotes(),
        source.getCreatedAt(),
        source.getSingleSources().stream()
            .sorted(Comparator.comparing(SingleSource::getTitle).reversed())
            .map(this::toSingleSourceResponse)
            .toList());
  }

  private SingleSourceResponse toSingleSourceResponse(SingleSource source) {
    return new SingleSourceResponse(
        source.getSingleSourceId(),
        source.getJournal(),
        source.getAuthor(),
        source.getTitle(),
        source.getUrl(),
        source.getDate());
  }
}

