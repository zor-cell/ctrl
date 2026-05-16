package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import at.ac.tuwien.inso.ctrl.post.dto.response.SingleSourceResponse;
import at.ac.tuwien.inso.ctrl.post.dto.response.SourceResponse;
import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportAction;
import at.ac.tuwien.inso.ctrl.report.dto.ReportSummaryResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedSourceResponse;
import at.ac.tuwien.inso.ctrl.report.dto.UpdateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {
  private final ReportRepository reportRepository;
  private final PostRepository postRepository;
  private final SourceRepository sourceRepository;
  private final PostVoteRepository postVoteRepository;
  private final SourceVoteRepository sourceVoteRepository;
  private final UserRepository userRepository;
  private final FeedMapper feedMapper;
  private final ExternalAuthorRepository externalAuthorRepository;
  private final GmailEmailService gmailEmailService;
  private final TemplateEngine templateEngine;
  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  @Override
  public void makeReport(CreateReportRequest createReportRequest, User currentUser) {
    User contentUser = getUserFromContent(createReportRequest);
    ExternalAuthor externalAuthor = null;
    if (contentUser == null) {
      externalAuthor = getExternalAuthorFromContent(createReportRequest);
    }

    Report newReport = Report.builder()
        .contentId(createReportRequest.contentId())
        .reason(createReportRequest.reason())
        .description(createReportRequest.description())
        .fromUser(currentUser)
        .usernameReported(contentUser)
        .externalAuthorReported(externalAuthor)
        .contentType(createReportRequest.contentType())
        .status(ReportStatus.CREATED)
        .build();
    reportRepository.save(newReport);
    
  }



  @Override
  public List<ReportedPostResponse> getReportedPosts(ReportStatus reportStatus, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());
    List<Report> reports;

    reports = reportRepository.findByContentTypeAndStatus(ContentTypeEnum.POST, reportStatus, pageable);

    List<String> postIds = reports.stream().map(Report::getContentId).toList();

    List<Post> posts = postRepository.findAllWithAssociationsByIds(postIds);

    Map<String, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getPostId, post -> post));

    List<String> sourceIds = posts.stream()
        .filter(p -> p.getSource() != null)
        .map(p -> p.getSource().getSourceId())
        .toList();

    Map<String, String> postVotes = Map.of();
    Map<String, String> sourceVotes = Map.of();

    return reports.stream()
        .map(report -> {
          Post post = postMap.get(report.getContentId());
          if (post != null) {
            return toReportedPostResponse(report, post, postVotes, sourceVotes);
          }
          return null;
        })
        .filter(r -> r != null)
        .toList();
  }

  @Override
  public void updateReportStatus(UpdateReportRequest request, String id) {
    Report report = reportRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    report.setStatus(request.status());
    reportRepository.save(report);

    if (request.action() == ReportAction.DELETE_ACCOUNT) {
      suspendAccount(report);
    } else if (request.action() == ReportAction.DELETE_ITEM) {
        deleteReportedItem(report);
    }

    sendEmailToReporter(report);

  }

  private void sendEmailToReporter(Report report) {
    if (!requireEmailVerification) {
      return;
    }

    if (report.getFromUser() == null) {
      throw new UsernameNotFoundException("Reporting user not found for report: " + report.getReportId());
    }
    try {

      Context context = new Context();
      context.setVariable("status", report.getStatus().name());
      context.setVariable("reportTime", formatReportTime(report.getCreatedAt()));

      String htmlContent = templateEngine.process("reportUpdate", context);
      gmailEmailService.sendHtmlEmail(report.getFromUser().getEmail(), "Your Report has been reviewed", htmlContent);
    } catch (MailException e) {
      throw new CommunicationException("Failed to send email.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email.");
    } catch (IOException e) {
      throw new CommunicationException("Failed to send or create email.");

    }
  }

  private String formatReportTime(LocalDateTime reportTime) {
    if (reportTime == null) {
      return "";
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    return reportTime.format(formatter);
  }

  @Override
  public List<ReportSummaryResponse> getAccountReportSummary(String reportId) {
    Report report = reportRepository.findById(reportId).orElseThrow(() -> new EntityNotFoundException("Report not found or not validated: " + reportId));

    User user = report.getUsernameReported();
    List<Report> reports;
    if (user != null) {
      reports = reportRepository.findAllByUsernameReportedAndStatus(user, ReportStatus.VALIDATED);
    } else {
      reports = reportRepository.findAllByExternalAuthorReportedAndStatus(report.getExternalAuthorReported(), ReportStatus.VALIDATED);
    }

    if (reports.isEmpty()) {
      return List.of();
    }

    return reports.stream().map(r -> new ReportSummaryResponse(
        r.getContentType(),
        r.getReason(),
        r.getDescription()
    )).toList();

  }

  private void deleteReportedItem(Report report) {
    if (report.getContentType() == ContentTypeEnum.POST) {
        Post post = postRepository.getPostByPostId(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Source not found: " + report.getContentId()));
        post.setIsRemoved(true);
        postRepository.save(post);

    } else if (report.getContentType() == ContentTypeEnum.SOURCE) {
        Source source = sourceRepository.findById(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Source not found: " + report.getContentId()));
        source.setIsRemoved(true);
        sourceRepository.save(source);
    }
  }

  private void suspendAccount(Report report) {
    if (report.getUsernameReported() != null) {
      User user = report.getUsernameReported();
      if (user != null) {
        user.setSuspended(true);
        userRepository.save(user);
      } else {
        ContentTypeEnum type = report.getContentType();
        if (type == ContentTypeEnum.POST) {
            Post post = postRepository.findById(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Post not found: " + report.getContentId()));
            ExternalAuthor postUser = post.getExternalAuthor();
            postUser.setSuspended(true);
            externalAuthorRepository.save(postUser);

        } else if (type == ContentTypeEnum.SOURCE) {
          Source source = sourceRepository.findById(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Source not found: " + report.getContentId()));


          Post post = postRepository.findBySource(source).orElseThrow(() ->
              new EntityNotFoundException("Post not found for source: " + source.getSourceId()));
          ExternalAuthor author = post.getExternalAuthor();
          author.setSuspended(true);
          externalAuthorRepository.save(author);
        }
      }

    }
  }

  @Override
  public List<ReportedSourceResponse> getReportedSources(ReportStatus status, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());
    List<Report> reports;

    reports = reportRepository.findByContentTypeAndStatus(ContentTypeEnum.SOURCE, status, pageable);

    return reports.stream().map(this::toReportedSourceResponse).toList();
  }

  private ReportedSourceResponse toReportedSourceResponse(Report report) {
    Source source = sourceRepository.findById(report.getContentId()).orElseThrow(() -> new EntityNotFoundException("Source not found: " + report.getContentId()));

    SourceResponse sourceResponse = toSourceResponse(source);

    return new ReportedSourceResponse(
        // Report Info
        report.getReportId(),
        report.getReason(),
        report.getDescription(),
        report.getFromUser().getUsername(),
        report.getUsernameReported() != null ? report.getUsernameReported().getUsername() : getUserBySource(source),
        report.getContentType(),
        report.getStatus(),
        report.getCreatedAt(),
        sourceResponse
    );
  }

  private String getUserBySource(Source source) {
    Post post = postRepository.findBySource(source).orElseThrow(() ->
        new EntityNotFoundException("Post not found for source: " + source.getSourceId()));
    return post.getExternalAuthor().getDisplayName();
  }

  private SourceResponse toSourceResponse(Source source) {
    if (source == null) {
      throw new EntityNotFoundException("Source not found");
    }

    List<SingleSourceResponse> singleSourceResponses =
        source.getSingleSources().stream()
            .sorted(
                Comparator.comparing(SingleSource::getTitle).reversed())
            .map(this::toSingleSourceResponse)
            .toList();

    return new SourceResponse(
        source.getSourceId(),
        source.getUpvotes(),
        source.getDownvotes(),
        source.getCreatedAt(),
        singleSourceResponses);
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

  private ReportedPostResponse toReportedPostResponse(
      Report report,
      Post post,
      Map<String, String> postVotes,
      Map<String, String> sourceVotes
  ) {

    // Use the same mapping logic as FeedService
    FeedPostResponse feedPostResponse = feedMapper.toFeedPostResponse(post, postVotes, sourceVotes);

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

  private ExternalAuthor getExternalAuthorFromContent(CreateReportRequest createReportRequest) {
    if (createReportRequest.contentType().equals(ContentTypeEnum.POST)) {
      Post post = postRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Post not found: " + createReportRequest.contentId()));
      return post.getExternalAuthor();

    } else if (createReportRequest.contentType().equals(ContentTypeEnum.SOURCE)) {

      Source source = sourceRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Source not found: " + createReportRequest.contentId()));
      Post post = postRepository.findBySource(source).orElseThrow(() ->
          new EntityNotFoundException("Post not found for source: " + createReportRequest.contentId()));
      return post.getExternalAuthor();

    } else {
      throw new IllegalArgumentException("Invalid content type: " + createReportRequest.contentType());
    }
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
