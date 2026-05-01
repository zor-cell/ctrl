package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {
  private final ReportRepository reportRepository;
  private final PostRepository postRepository;
  private final SourceRepository sourceRepository;
  private final FeedService feedService;

  @Override
  public void makeReport(CreateReportRequest createReportRequest, User currentUser) {
    User contentUser = getUserFromContent(createReportRequest);

    Report newReport = Report.builder()
        .contentId(createReportRequest.contentId())
        .reason(createReportRequest.reason())
        .description(createReportRequest.description())
        .fromUser(currentUser)
        .usernameReported(contentUser)
        .contentType(createReportRequest.contentType())
        .status(ReportStatus.CREATED)
        .build();
    reportRepository.save(newReport);
    
  }

  @Override
  public List<ReportedPostResponse> getReported(ContentTypeEnum contentType, ReportStatus reportStatus, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());
    List<Report> reports;

    reports = reportRepository.findByContentTypeAndStatus(contentType, reportStatus, pageable);

    return reports.stream().map(this::toReportedPostResponse).toList();
  }

  private ReportedPostResponse toReportedPostResponse(Report report) {
    Post post = postRepository.findById(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Post not found: " + report.getContentId()));

    FeedPostResponse feedPostResponse = feedService.toFeedPostResponse(post, null);

    return new ReportedPostResponse(
        // Report Info
        report.getReportId(),
        report.getReason(),
        report.getDescription(),
        report.getFromUser().getUsername(),
        report.getUsernameReported() != null ? report.getUsernameReported().getUsername() : feedPostResponse.username(),
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
        feedPostResponse.mediaUrl(),
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

  private User getUserFromContent(CreateReportRequest createReportRequest) {
    if (createReportRequest.contentType().equals(ContentTypeEnum.POST)) {
      Post post = postRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Post not found: " + createReportRequest.contentId()));
      
      return post.getUser();

    } else if (createReportRequest.contentType().equals(ContentTypeEnum.SOURCE)) {

      Source source = sourceRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Source not found: " + createReportRequest.contentId()));
      Post post = postRepository.findBySource(source).orElseThrow(() ->
          new EntityNotFoundException("Post not found for source: " + createReportRequest.contentId()));
      return post.getUser();

    } else {
      throw new IllegalArgumentException("Invalid content type: " + createReportRequest.contentType());
    }
  }
}
