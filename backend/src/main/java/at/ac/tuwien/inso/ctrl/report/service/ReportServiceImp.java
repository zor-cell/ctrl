package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.mapper.CommentMapper;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportAction;
import at.ac.tuwien.inso.ctrl.report.dto.ReportSummaryResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedAccountResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedCommentResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedPostResponse;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedSourceResponse;
import at.ac.tuwien.inso.ctrl.report.dto.UpdateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.mapper.ReportMapper;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.mapper.UserMapper;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {
  private final ReportRepository reportRepository;
  private final PostRepository postRepository;
  private final SourceRepository sourceRepository;
  private final UserRepository userRepository;
  private final ExternalAuthorRepository externalAuthorRepository;
  private final GmailEmailService gmailEmailService;
  private final TemplateEngine templateEngine;
  private final CommentRepository commentRepository;
  private final CommentMapper commentMapper;
  private final UserMapper userMapper;
  private final ReportReasonLabelResolver reportReasonLabelResolver;
  private final ReportMapper reportMapper;

  @Value("${application.auth.require-email-verification:true}")
  private boolean requireEmailVerification;

  @Override
  public void makeReport(CreateReportRequest createReportRequest, User currentUser) {
    User contentUser = getUserFromContent(createReportRequest);
    ExternalAuthor externalAuthor = null;
    if (contentUser == null) {
      externalAuthor = getExternalAuthorFromContent(createReportRequest);
    }

    Report newReport =
        Report.builder()
            .contentId(createReportRequest.contentId())
            .reason(createReportRequest.reason())
            .description(createReportRequest.description())
            .fromUser(currentUser)
            .usernameReported(contentUser)
            .externalAuthorReported(externalAuthor)
            .contentType(createReportRequest.contentType())
            .status(ReportStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .build();
    reportRepository.save(newReport);
  }

  @Override
  public List<ReportedPostResponse> getReportedPosts(
      ReportStatus reportStatus, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());
    List<Report> reports;

    reports =
        reportRepository.findByContentTypeAndStatus(ContentTypeEnum.POST, reportStatus, pageable);

    List<String> postIds = reports.stream().map(Report::getContentId).toList();

    List<Post> posts = postRepository.findAllWithAssociationsByIds(postIds);

    Map<String, Post> postMap =
        posts.stream().collect(Collectors.toMap(Post::getPostId, post -> post));

    Map<String, String> postVotes = Map.of();
    Map<String, String> sourceVotes = Map.of();

    return reports.stream()
        .map(
            report -> {
              Post post = postMap.get(report.getContentId());
              if (post != null) {
                return reportMapper.toReportedPostResponse(report, post, postVotes, sourceVotes);
              }
              return null;
            })
        .filter(r -> r != null)
        .toList();
  }

  @Override
  public List<ReportedCommentResponse> getReportedComments(
      ReportStatus reportStatus, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());

    List<Report> reports =
        reportRepository.findByContentTypeAndStatus(
            ContentTypeEnum.COMMENT, reportStatus, pageable);

    List<String> commentIds = reports.stream().map(Report::getContentId).distinct().toList();
    List<Comment> comments = commentRepository.findAllById(commentIds);

    Map<String, Comment> commentMap =
        comments.stream().collect(Collectors.toMap(Comment::getCommentId, comment -> comment));

    Map<String, List<Comment>> repliesByParent = Map.of();
    return reports.stream()
        .map(
            report -> {
              Comment comment = commentMap.get(report.getContentId());
              if (comment == null) {
                return null;
              }

              CommentResponse commentResponse =
                  commentMapper.toResponse(comment, repliesByParent, Map.of());
              String reportedByUsername =
                  report.getFromUser() != null ? report.getFromUser().getUsername() : null;
              String reportedUsername =
                  report.getUsernameReported() != null
                      ? report.getUsernameReported().getUsername()
                      : comment.getUser().getUsername();

              return new ReportedCommentResponse(
                  commentResponse,
                  report.getReportId(),
                  report.getReason(),
                  report.getDescription(),
                  reportedByUsername,
                  reportedUsername,
                  report.getContentType(),
                  report.getStatus(),
                  report.getCreatedAt());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  @Override
  public void updateReportStatus(UpdateReportRequest request, String id) {
    Report report = reportRepository.findById(id).orElseThrow(EntityNotFoundException::new);
    if (report.getCreatedAt() == null) {
      report.setCreatedAt(LocalDateTime.now());
    }
    report.setStatus(request.status());
    if (request.description() != null) {
      report.setDescription(request.description());
    }
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
      throw new UsernameNotFoundException(
          "Reporting user not found for report: " + report.getReportId());
    }
    try {

      Context context = new Context();
      context.setVariable("status", report.getStatus().name());
      context.setVariable("reportTime", formatReportTime(report.getCreatedAt()));

      String htmlContent = templateEngine.process("reportUpdate", context);
      gmailEmailService.sendHtmlEmail(
          report.getFromUser().getEmail(), "Your Report has been reviewed", htmlContent);
    } catch (MailException e) {
      throw new CommunicationException("Failed to send email.");
    } catch (MessagingException me) {
      throw new CommunicationException("Failed to create email.");
    } catch (IOException e) {
      throw new CommunicationException("Failed to send or create email.");
    }
  }

  private void sendEmailToReported(Report report, ReportAction action) {
    if (!requireEmailVerification) {
      return;
    }

    if (report.getUsernameReported() == null) {
      return;
    }
    try {

      Context context = new Context();
      if (action == ReportAction.DELETE_ACCOUNT) {
        context.setVariable("type", ContentTypeEnum.ACCOUNT.name());
      } else {
        context.setVariable("type", report.getContentType().name());
      }

      context.setVariable("reasonLabel", reportReasonLabelResolver.resolve(report.getReason()));

      String htmlContent = templateEngine.process("actionNotificationTemplate", context);
      gmailEmailService.sendHtmlEmail(
          report.getUsernameReported().getEmail(),
          "An action has been taken against you",
          htmlContent);
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
    Report report =
        reportRepository
            .findById(reportId)
            .orElseThrow(
                () ->
                    new EntityNotFoundException("Report not found or not validated: " + reportId));

    User user = report.getUsernameReported();
    List<Report> reports;
    if (user != null) {
      reports = reportRepository.findAllByUsernameReportedAndStatus(user, ReportStatus.VALIDATED);
    } else {
      reports =
          reportRepository.findAllByExternalAuthorReportedAndStatus(
              report.getExternalAuthorReported(), ReportStatus.VALIDATED);
    }

    if (reports.isEmpty()) {
      return List.of();
    }

    return reports.stream()
        .map(r -> new ReportSummaryResponse(r.getContentType(), r.getReason(), r.getDescription()))
        .toList();
  }

  @Override
  public List<ReportedAccountResponse> getAccountReports(
      ReportStatus reportStatus, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());

    List<Report> reports =
        reportRepository.findByContentTypeAndStatus(
            ContentTypeEnum.ACCOUNT, reportStatus, pageable);

    return reports.stream()
        .map(
            report -> {
              User reportedUser = report.getUsernameReported();
              if (reportedUser == null) {
                return null;
              }

              String reportedByUsername = report.getFromUser().getUsername();
              String reportedUsername = reportedUser.getUsername();

              return new ReportedAccountResponse(
                  userMapper.toPublicUserProfileResponse(reportedUser, true, false),
                  report.getReportId(),
                  report.getReason(),
                  report.getDescription(),
                  reportedByUsername,
                  reportedUsername,
                  report.getContentType(),
                  report.getStatus(),
                  report.getCreatedAt());
            })
        .filter(Objects::nonNull)
        .toList();
  }

  private void dealWithOtherReportsOfItem(ContentTypeEnum type, String id) {

    List<Report> reports = reportRepository.findAllByContentIdAndContentType(id, type);
    for (Report report : reports) {
      if (report.getStatus() == ReportStatus.CREATED) {
        report.setStatus(ReportStatus.OTHER_REPORT);
        reportRepository.save(report);
      }
    }
  }

  private void deleteReportedItem(Report report) {
    if (report.getContentType() == ContentTypeEnum.POST) {
      Post post =
          postRepository
              .getPostByPostId(report.getContentId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Source not found: " + report.getContentId()));
      post.setIsRemoved(true);
      postRepository.save(post);

      sendEmailToReported(report, ReportAction.DELETE_ITEM);

    } else if (report.getContentType() == ContentTypeEnum.SOURCE) {
      Source source =
          sourceRepository
              .findById(report.getContentId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Source not found: " + report.getContentId()));
      source.setIsRemoved(true);
      sourceRepository.save(source);
      sendEmailToReported(report, ReportAction.DELETE_ITEM);

    } else if (report.getContentType() == ContentTypeEnum.COMMENT) {
      Comment comment =
          commentRepository
              .findById(report.getContentId())
              .orElseThrow(
                  () -> new EntityNotFoundException("Comment not found: " + report.getContentId()));
      comment.setIsRemoved(true);
      commentRepository.save(comment);
      sendEmailToReported(report, ReportAction.DELETE_ITEM);

    } else if (report.getContentType() == ContentTypeEnum.ACCOUNT) {
      User user = report.getUsernameReported();

      if (user != null) {
        user.setSuspended(true);
        userRepository.save(user);
        dealWithOtherReportsOfUser(user);
        sendEmailToReported(report, ReportAction.DELETE_ITEM);

      } else {
        throw new IllegalArgumentException("User cannot be null for reported User");
      }
    }

    dealWithOtherReportsOfItem(report.getContentType(), report.getContentId());
  }

  private void suspendAccount(Report report) {
    User user = report.getUsernameReported();
    if (user != null) {
      user.setSuspended(true);
      userRepository.save(user);
      dealWithOtherReportsOfUser(user);
      sendEmailToReported(report, ReportAction.DELETE_ACCOUNT);
    } else {
      ContentTypeEnum type = report.getContentType();
      if (type == ContentTypeEnum.POST) {

        Post post =
            postRepository
                .findById(report.getContentId())
                .orElseThrow(
                    () -> new EntityNotFoundException("Post not found: " + report.getContentId()));
        ExternalAuthor postUser = post.getExternalAuthor();
        postUser.setSuspended(true);
        externalAuthorRepository.save(postUser);
        dealWithOtherReportsOfExternalAuthor(postUser);

      } else if (type == ContentTypeEnum.SOURCE) {

        Source source =
            sourceRepository
                .findById(report.getContentId())
                .orElseThrow(
                    () ->
                        new EntityNotFoundException("Source not found: " + report.getContentId()));
        Post post =
            postRepository
                .findBySource(source)
                .orElseThrow(
                    () ->
                        new EntityNotFoundException(
                            "Post not found for source: " + source.getSourceId()));
        ExternalAuthor author = post.getExternalAuthor();
        author.setSuspended(true);
        externalAuthorRepository.save(author);
        dealWithOtherReportsOfExternalAuthor(author);

      } else if (type == ContentTypeEnum.COMMENT) {
        Comment comment =
            commentRepository
                .findById(report.getContentId())
                .orElseThrow(
                    () ->
                        new EntityNotFoundException("Comment not found: " + report.getContentId()));
        User commentUser = comment.getUser();
        commentUser.setSuspended(true);
        userRepository.save(commentUser);
        dealWithOtherReportsOfUser(commentUser);
      } else {
        throw new IllegalArgumentException("User cannot be null for reported User");
      }
    }
  }

  private void dealWithOtherReportsOfUser(User user) {
    List<Report> reports =
        reportRepository.findAllByUsernameReportedAndStatus(user, ReportStatus.CREATED);
    for (Report report : reports) {
      if (report.getStatus() == ReportStatus.CREATED) {
        report.setStatus(ReportStatus.OTHER_REPORT);
        reportRepository.save(report);
      }
    }
  }

  private void dealWithOtherReportsOfExternalAuthor(ExternalAuthor externalAuthor) {
    List<Report> reports =
        reportRepository.findAllByExternalAuthorReportedAndStatus(
            externalAuthor, ReportStatus.CREATED);
    for (Report report : reports) {
      if (report.getStatus() == ReportStatus.CREATED) {
        report.setStatus(ReportStatus.OTHER_REPORT);
        reportRepository.save(report);
      }
    }
  }

  @Override
  public List<ReportedSourceResponse> getReportedSources(
      ReportStatus status, int page, int postsPerPage) {
    var pageable = PageRequest.of(page, postsPerPage, Sort.by("createdAt").ascending());
    List<Report> reports;

    reports = reportRepository.findByContentTypeAndStatus(ContentTypeEnum.SOURCE, status, pageable);

    return reports.stream().map(reportMapper::toReportedSourceResponse).toList();
  }

  @Override
  public boolean wasReportedByUser(User user, ContentTypeEnum type, String contentId) {
    return reportRepository.existsReportByFromUserAndContentTypeAndContentId(user, type, contentId);
  }

  private User getUserFromContent(CreateReportRequest createReportRequest) {
    String contentId = createReportRequest.contentId();
    if (createReportRequest.contentType() == ContentTypeEnum.POST) {
      return postRepository
          .getPostByPostId(contentId)
          .orElseThrow(() -> new EntityNotFoundException("Post not found: " + contentId))
          .getUser();
    } else if (createReportRequest.contentType() == ContentTypeEnum.SOURCE) {
      Source source =
          sourceRepository
              .findById(contentId)
              .orElseThrow(() -> new EntityNotFoundException("Source not found: " + contentId));
      Post post =
          postRepository
              .findBySource(source)
              .orElseThrow(
                  () -> new EntityNotFoundException("Post not found for source: " + contentId));
      return post.getUser();

    } else if (createReportRequest.contentType() == ContentTypeEnum.COMMENT) {
      return commentRepository.findById(contentId).map(Comment::getUser).orElse(null);

    } else {

      return userRepository
          .findById(UUID.fromString(contentId))
          .orElseThrow(() -> new EntityNotFoundException("User not found: " + contentId));
    }
  }

  private ExternalAuthor getExternalAuthorFromContent(CreateReportRequest createReportRequest) {
    String contentId = createReportRequest.contentId();
    if (createReportRequest.contentType() == ContentTypeEnum.POST) {
      return postRepository
          .getPostByPostId(contentId)
          .map(Post::getExternalAuthor)
          .orElseThrow(() -> new EntityNotFoundException("Post not found: " + contentId));
    } else if (createReportRequest.contentType() == ContentTypeEnum.SOURCE) {
      Source source =
          sourceRepository
              .findById(contentId)
              .orElseThrow(() -> new EntityNotFoundException("Source not found: " + contentId));
      Post post =
          postRepository
              .findBySource(source)
              .orElseThrow(
                  () -> new EntityNotFoundException("Post not found for source: " + contentId));
      return post.getExternalAuthor();

    } else {
      throw new IllegalArgumentException(
          "Invalid content type: " + createReportRequest.contentType());
    }
  }
}
