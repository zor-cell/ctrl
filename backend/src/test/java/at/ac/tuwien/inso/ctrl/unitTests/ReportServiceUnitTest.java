package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.comments.dto.response.CommentResponse;
import at.ac.tuwien.inso.ctrl.comments.mapper.CommentMapper;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.dto.ReportedCommentResponse;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.mapper.ReportMapper;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.report.service.ReportReasonLabelResolver;
import at.ac.tuwien.inso.ctrl.report.service.ReportServiceImp;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
public class ReportServiceUnitTest {
  @Mock private ReportRepository reportRepository;
  @Mock private PostRepository postRepository;
  @Mock private SourceRepository sourceRepository;
  @Mock private CommentRepository commentRepository;
  @Mock private PostVoteRepository postVoteRepository;
  @Mock private SourceVoteRepository sourceVoteRepository;
  @Mock private UserRepository userRepository;
  @Mock private FeedMapper feedMapper;
  @Mock private ExternalAuthorRepository externalAuthorRepository;
  @Mock private GmailEmailService gmailEmailService;
  @Mock private TemplateEngine templateEngine;
  @Mock private CommentMapper commentMapper;

  @InjectMocks private ReportServiceImp reportService;
  @InjectMocks private ReportMapper reportMapper;

  private final ReportReasonLabelResolver reportReasonLabelResolver =
      new ReportReasonLabelResolver();

  @Test
  void formatTime_WithReportWithNoTime_shouldReturnEmptyString()
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method =
        ReportServiceImp.class.getDeclaredMethod("formatReportTime", LocalDateTime.class);
    method.setAccessible(true);

    String returnVal = (String) method.invoke(reportService, new Object[] {null});

    Assertions.assertEquals("", returnVal);
  }

  @Test
  void toSourceResponse_withNullSource_shouldThrowException() throws NoSuchMethodException {
    Method method = ReportMapper.class.getDeclaredMethod("toSourceResponse", Source.class);
    method.setAccessible(true);

    InvocationTargetException thrown =
        Assertions.assertThrows(
            InvocationTargetException.class,
            () -> method.invoke(reportMapper, new Object[] {null}));

    Assertions.assertInstanceOf(EntityNotFoundException.class, thrown.getCause());
  }

  @Test
  void resolveReason_WithNullReason_shouldReturnNull() {
    String reason = reportReasonLabelResolver.resolve(null);
    assertEquals("", reason);
  }

  @Test
  void resolveReason_WithBlankReason_shouldReturnNull() {
    String reason = reportReasonLabelResolver.resolve("");
    assertEquals("", reason);
  }

  @Test
  void getReportedComments_skipsMissingComments() {
    User reporter = new User();
    reporter.setUsername("reporter");

    User commentUser = new User();
    commentUser.setUsername("commentUser");

    Comment comment = Comment.builder().commentId("comment-1").user(commentUser).build();

    Report validReport =
        Report.builder()
            .reportId("report-1")
            .fromUser(reporter)
            .usernameReported(commentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId("comment-1")
            .reason("spam")
            .description("desc")
            .status(ReportStatus.CREATED)
            .build();

    Report missingReport =
        Report.builder()
            .reportId("report-2")
            .fromUser(reporter)
            .usernameReported(commentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId("missing-comment")
            .reason("spam")
            .description("desc")
            .status(ReportStatus.CREATED)
            .build();

    Mockito.when(
            reportRepository.findByContentTypeAndStatus(
                Mockito.eq(ContentTypeEnum.COMMENT),
                Mockito.eq(ReportStatus.CREATED),
                Mockito.any()))
        .thenReturn(List.of(validReport, missingReport));
    Mockito.when(commentRepository.findAllById(Mockito.anyList())).thenReturn(List.of(comment));
    Mockito.when(
            commentMapper.toResponse(Mockito.eq(comment), Mockito.anyMap(), Mockito.eq(Map.of())))
        .thenReturn(
            new CommentResponse(
                "comment-1",
                null,
                "commentUser",
                null,
                null,
                "content",
                null,
                false,
                false,
                List.of(),
                false));

    List<ReportedCommentResponse> responses =
        reportService.getReportedComments(ReportStatus.CREATED, 0, 10);

    Assertions.assertEquals(1, responses.size());
    Assertions.assertEquals("comment-1", responses.get(0).comment().commentId());
  }

  @Test
  void getReportedComments_resolvesReporterAndReportedUsernames() {
    User reporter = new User();
    reporter.setUsername("reporter");

    User reportedUser = new User();
    reportedUser.setUsername("reportedUser");

    User commentUser = new User();
    commentUser.setUsername("commentUser");

    Comment commentWithReportedUser =
        Comment.builder().commentId("comment-2").user(commentUser).build();
    Comment commentWithFallbackUser =
        Comment.builder().commentId("comment-3").user(commentUser).build();

    Report reportWithReportedUser =
        Report.builder()
            .reportId("report-3")
            .fromUser(reporter)
            .usernameReported(reportedUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId("comment-2")
            .reason("spam")
            .description("desc")
            .status(ReportStatus.CREATED)
            .build();

    Report reportWithFallbackUser =
        Report.builder()
            .reportId("report-4")
            .fromUser(null)
            .usernameReported(null)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId("comment-3")
            .reason("spam")
            .description("desc")
            .status(ReportStatus.CREATED)
            .build();

    Mockito.when(
            reportRepository.findByContentTypeAndStatus(
                Mockito.eq(ContentTypeEnum.COMMENT),
                Mockito.eq(ReportStatus.CREATED),
                Mockito.any()))
        .thenReturn(List.of(reportWithReportedUser, reportWithFallbackUser));
    Mockito.when(commentRepository.findAllById(Mockito.anyList()))
        .thenReturn(List.of(commentWithReportedUser, commentWithFallbackUser));
    Mockito.when(
            commentMapper.toResponse(
                Mockito.eq(commentWithReportedUser), Mockito.anyMap(), Mockito.eq(Map.of())))
        .thenReturn(
            new CommentResponse(
                "comment-2",
                null,
                "commentUser",
                null,
                null,
                "content",
                null,
                false,
                false,
                List.of(),
                false));
    Mockito.when(
            commentMapper.toResponse(
                Mockito.eq(commentWithFallbackUser), Mockito.anyMap(), Mockito.eq(Map.of())))
        .thenReturn(
            new CommentResponse(
                "comment-3",
                null,
                "commentUser",
                null,
                null,
                "content",
                null,
                false,
                false,
                List.of(),
                false));

    List<ReportedCommentResponse> responses =
        reportService.getReportedComments(ReportStatus.CREATED, 0, 10);

    ReportedCommentResponse first = responses.get(0);
    ReportedCommentResponse second = responses.get(1);

    Assertions.assertEquals("reporter", first.reportedByUsername());
    Assertions.assertEquals("reportedUser", first.reportedUsername());
    Assertions.assertNull(second.reportedByUsername());
    Assertions.assertEquals("commentUser", second.reportedUsername());
  }
}
