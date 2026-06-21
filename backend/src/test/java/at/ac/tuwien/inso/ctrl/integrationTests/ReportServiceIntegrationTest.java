package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.exception.CommunicationException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportAction;
import at.ac.tuwien.inso.ctrl.report.dto.ReportSummaryResponse;
import at.ac.tuwien.inso.ctrl.report.dto.UpdateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.mapper.ReportMapper;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mail.MailSendException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
public class ReportServiceIntegrationTest {

  @Autowired private ReportService reportService;
  @Autowired private ReportRepository reportRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private SourceRepository sourceRepository;
  @Autowired private ExternalAuthorRepository externalAuthorRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private ReportMapper reportMapper;

  @MockitoBean private GmailEmailService gmailEmailService;
  @MockitoBean private FileStorageService fileStorageService;

  @BeforeEach
  void setUp() {
    Mockito.when(fileStorageService.resolveFileUrl(Mockito.anyString()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @AfterEach
  void tearDown() {
    reportRepository.deleteAll();
    commentRepository.deleteAll();
    postRepository.deleteAll();
    sourceRepository.deleteAll();
    externalAuthorRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  void makeReport_withInternalPost_savesReportWithUser() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Post post = createPost("post-1", contentUser, null, null);

    CreateReportRequest request =
        new CreateReportRequest(
            "spam", "bad content", post.getPostId(), ContentTypeEnum.POST, reporter.getUsername());

    reportService.makeReport(request, reporter);

    Report savedReport = reportRepository.findAll().get(0);
    assertEquals(ReportStatus.CREATED, savedReport.getStatus());
    assertEquals(contentUser.getId(), savedReport.getUsernameReported().getId());
    assertEquals(post.getPostId(), savedReport.getContentId());
    assertNotNull(savedReport.getCreatedAt());
  }

  @Test
  void makeReport_withExternalAuthorPost_savesReportWithExternalAuthor() {
    User reporter = createUser("reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-1");
    Post post = createPost("post-2", null, externalAuthor, null);

    CreateReportRequest request =
        new CreateReportRequest(
            "spam", "bad content", post.getPostId(), ContentTypeEnum.POST, reporter.getUsername());

    reportService.makeReport(request, reporter);

    Report savedReport = reportRepository.findAll().get(0);
    assertNotNull(savedReport.getExternalAuthorReported());
    assertEquals(
        externalAuthor.getExternalAuthorId(),
        savedReport.getExternalAuthorReported().getExternalAuthorId());
    assertEquals(post.getPostId(), savedReport.getContentId());
  }

  @Test
  void getReportedPosts_filtersMissingPosts() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-1");
    Post post = createPost("post-3", contentUser, null, source);

    createReport(
        reporter,
        contentUser,
        null,
        ContentTypeEnum.POST,
        post.getPostId(),
        ReportStatus.CREATED,
        null);
    createReport(
        reporter,
        contentUser,
        null,
        ContentTypeEnum.POST,
        "missing-post",
        ReportStatus.CREATED,
        null);

    var responses = reportService.getReportedPosts(ReportStatus.CREATED, 0, 10);

    assertEquals(1, responses.size());
    assertEquals(post.getPostId(), responses.get(0).postId());
  }

  @Test
  void getReportedSources_returnsReportedSources() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-2");

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    var responses = reportService.getReportedSources(ReportStatus.CREATED, 0, 10);

    assertEquals(1, responses.size());
    assertEquals(report.getReportId(), responses.get(0).reportId());
    assertEquals(source.getSourceId(), responses.get(0).sourceResponse().sourceId());
  }

  @Test
  void updateReportStatus_deleteItemPost_marksPostRemoved_andBackfillsTime() throws Exception {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Post post = createPost("post-4", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);
    report.setCreatedAt(null);
    reportRepository.save(report);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    Post updatedPost = postRepository.findById(post.getPostId()).orElseThrow();
    Report updatedReport = reportRepository.findById(report.getReportId()).orElseThrow();

    assertTrue(updatedPost.getIsRemoved());
    assertNotNull(updatedReport.getCreatedAt());
    Mockito.verify(gmailEmailService)
        .sendHtmlEmail(Mockito.eq(reporter.getEmail()), Mockito.anyString(), Mockito.anyString());
  }

  @Test
  void updateReportStatus_deleteItemSource_marksSourceRemoved() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-3");

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    Source updatedSource = sourceRepository.findById(source.getSourceId()).orElseThrow();
    assertTrue(updatedSource.getIsRemoved());
  }

  @Test
  void updateReportStatus_dismiss_marksReportAsDismissed() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-3");

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.DECLINED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    Report r = reportRepository.findById(report.getReportId()).orElseThrow();
    assertEquals(ReportStatus.DECLINED, r.getStatus());
  }

  @Test
  void updateReportStatus_deleteItemSource_marksSourceRemovedAndOtherSourceAsOtherReport() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-3");

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    Report report2 =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    Source updatedSource = sourceRepository.findById(source.getSourceId()).orElseThrow();
    assertTrue(updatedSource.getIsRemoved());

    Report report1FromRepo = reportRepository.findById(report.getReportId()).orElseThrow();
    Report report2FromRepo = reportRepository.findById(report2.getReportId()).orElseThrow();

    assertEquals(ReportStatus.VALIDATED, report1FromRepo.getStatus());
    assertEquals(ReportStatus.OTHER_REPORT, report2FromRepo.getStatus());
  }

  @Test
  void updateReportStatus_deleteAccountOfComment_anonymizesUser() {
    User reporter = createUser("comment-account-reporter");
    User commentUser = createUser("comment-account-user");
    Post post = createPost("post-comment-account-1", commentUser, null, null);
    Comment comment = createComment("comment-account-1", post, commentUser);

    Report report =
        createReport(
            reporter,
            null,
            null,
            ContentTypeEnum.COMMENT,
            comment.getCommentId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    User updatedUser = userRepository.findById(commentUser.getId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
    assertTrue(updatedUser.isDeleted());
    assertEquals("Deleted user", updatedUser.getName());
  }

  @Test
  void updateReportStatus_suspendAccountOfCommentWithInvalidId_shouldThrowException() {
    User reporter = createUser("comment-account-reporter");
    User commentUser = createUser("comment-account-user");
    Post post = createPost("post-comment-account-1", commentUser, null, null);
    Comment comment = createComment("comment-account-1", post, commentUser);

    Report report =
        createReport(
            reporter, null, null, ContentTypeEnum.COMMENT, "id", ReportStatus.CREATED, null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_suspendAccountOfUser_throwException() {
    User reporter = createUser("comment-account-reporter");
    User commentUser = createUser("comment-account-user");
    Post post = createPost("post-comment-account-1", commentUser, null, null);
    Comment comment = createComment("comment-account-1", post, commentUser);

    Report report =
        createReport(
            reporter, null, null, ContentTypeEnum.COMMENT, "id", ReportStatus.CREATED, null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_deleteItemComment_marksSourceRemoved() {
    User reporter = createUser("comment-reporter");
    User commentUser = createUser("comment-user");
    Post post = createPost("post-comment-1", commentUser, null, null);
    Comment comment = createComment("comment-1", post, commentUser);

    Report report =
        createReport(
            reporter,
            commentUser,
            null,
            ContentTypeEnum.COMMENT,
            comment.getCommentId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    Comment updatedComment = commentRepository.findById(comment.getCommentId()).orElseThrow();
    assertTrue(updatedComment.getIsRemoved());
  }

  @Test
  void updateReportStatus_deleteItemAccount_marksAccountSuspended() {
    User reporter = createUser("comment-reporter");
    User accountUser = createUser("comment-user");

    Report report =
        createReport(
            reporter,
            accountUser,
            null,
            ContentTypeEnum.ACCOUNT,
            String.valueOf(accountUser.getId()),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
        report.getReportId());

    User updatedUser = userRepository.findById(accountUser.getId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
    assertTrue(updatedUser.isDeleted());
    assertEquals("Deleted user", updatedUser.getName());
  }

  @Test
  void updateReportStatus_deleteItemAccountWithInvalidPostId_throwsException() {
    User reporter = createUser("comment-reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");

    Report report =
        createReport(
            reporter, null, externalAuthor, ContentTypeEnum.POST, "id", ReportStatus.CREATED, null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_deleteItemAccountWithInvalidSourceId_throwsException() {
    User reporter = createUser("comment-reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            "id",
            ReportStatus.CREATED,
            null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_deleteItemAccountWithInvalidCommentId_throwsException() {
    User reporter = createUser("comment-reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.COMMENT,
            "id",
            ReportStatus.CREATED,
            null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_deleteItemAccountWithExternalAuthor_throwsException() {
    User reporter = createUser("comment-reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.ACCOUNT,
            "id",
            ReportStatus.CREATED,
            null);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void
      updateReportStatus_SuspendSourceWithExternalAuthor_marksSourceRemovedAndOtherSourceAsOtherReport() {
    User reporter = createUser("reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");
    Source source = createSource("source-3");
    Post post = createPost("post-ext-2", null, externalAuthor, source);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    Report report2 =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    Report report1FromRepo = reportRepository.findById(report.getReportId()).orElseThrow();
    Report report2FromRepo = reportRepository.findById(report2.getReportId()).orElseThrow();

    assertEquals(ReportStatus.VALIDATED, report1FromRepo.getStatus());
    assertEquals(ReportStatus.OTHER_REPORT, report2FromRepo.getStatus());
  }

  @Test
  @Disabled(
      "User deletion removes reports via reportRepository.deleteByUsernameReported, so this assertion is currently not valid")
  void updateReportStatus_SuspendSource_marksSourceRemovedAndOtherSourceAsOtherReport() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Source source = createSource("source-3");

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    Report report2 =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    Report report1FromRepo = reportRepository.findById(report.getReportId()).orElseThrow();
    Report report2FromRepo = reportRepository.findById(report2.getReportId()).orElseThrow();

    assertEquals(ReportStatus.VALIDATED, report1FromRepo.getStatus());
    assertEquals(ReportStatus.OTHER_REPORT, report2FromRepo.getStatus());
  }

  @Test
  void updateReportStatus_deleteAccount_suspendsReportedUser() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Post post = createPost("post-5", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    User updatedUser = userRepository.findById(contentUser.getId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
    assertTrue(updatedUser.isDeleted());
    assertEquals("Deleted user", updatedUser.getName());
  }

  @Test
  void getAccountReportSummary_returnsEmptyWhenNoValidatedReports() {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Post post = createPost("post-6", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    List<ReportSummaryResponse> summary =
        reportService.getAccountReportSummary(report.getReportId());

    assertTrue(summary.isEmpty());
  }

  @Test
  void getAccountReportSummary_returnsExternalAuthorReports() {
    User reporter = createUser("reporter");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-2");
    Post post = createPost("post-7", null, externalAuthor, null);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.VALIDATED,
            LocalDateTime.now());
    createReport(
        reporter,
        null,
        externalAuthor,
        ContentTypeEnum.POST,
        post.getPostId(),
        ReportStatus.VALIDATED,
        LocalDateTime.now());

    List<ReportSummaryResponse> summary =
        reportService.getAccountReportSummary(report.getReportId());

    assertEquals(2, summary.size());
  }

  @Test
  void updateReportStatus_whenMailSendFails_throwsCommunicationException() throws Exception {
    User reporter = createUser("reporter");
    User contentUser = createUser("contentUser");
    Post post = createPost("post-mail-1", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    Mockito.doThrow(new MailSendException("mail failure"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.eq(reporter.getEmail()), Mockito.anyString(), Mockito.anyString());

    assertThrows(
        CommunicationException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_whenMessagingFails_throwsCommunicationException() throws Exception {
    User reporter = createUser("reporter2");
    User contentUser = createUser("contentUser2");
    Post post = createPost("post-mail-2", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    Mockito.doThrow(new MessagingException("messaging failure"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.eq(reporter.getEmail()), Mockito.anyString(), Mockito.anyString());

    assertThrows(
        CommunicationException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void updateReportStatus_whenIOExceptionOccurs_throwsCommunicationException() throws Exception {
    User reporter = createUser("reporter3");
    User contentUser = createUser("contentUser3");
    Post post = createPost("post-mail-3", contentUser, null, null);

    Report report =
        createReport(
            reporter,
            contentUser,
            null,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    Mockito.doThrow(new IOException("io failure"))
        .when(gmailEmailService)
        .sendHtmlEmail(Mockito.eq(reporter.getEmail()), Mockito.anyString(), Mockito.anyString());

    assertThrows(
        CommunicationException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null),
                report.getReportId()));
  }

  @Test
  void getAccountReportSummary_withNonExistentReportId_throwsException() {
    assertThrows(
        EntityNotFoundException.class,
        () -> reportService.getAccountReportSummary("ID that does not exist"));
  }

  @Test
  void
      updateReportStatus_withReportedPostWithExternalAuthorAndNoUser_shouldSuspendExternalAuthor() {
    User reporter = createUser("reporter-external-post");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");
    Post post = createPost("post-ext-1", null, externalAuthor, null);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    ExternalAuthor updatedAuthor =
        externalAuthorRepository.findById(externalAuthor.getExternalAuthorId()).orElseThrow();
    assertTrue(updatedAuthor.isSuspended());
  }

  @Test
  void
      updateReportStatus_withReportedPostWithExternalAuthorAndNoUser_shouldSuspendExternalAuthorAndNotSendEmail() {
    User reporter = createUser("reporter-external-post");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-post-1");
    Post post = createPost("post-ext-1", null, externalAuthor, null);
    ReflectionTestUtils.setField(reportService, "requireEmailVerification", false);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.POST,
            post.getPostId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    ExternalAuthor updatedAuthor =
        externalAuthorRepository.findById(externalAuthor.getExternalAuthorId()).orElseThrow();
    assertTrue(updatedAuthor.isSuspended());

    ReflectionTestUtils.setField(reportService, "requireEmailVerification", true);
  }

  @Test
  void
      updateReportStatus_withReportedSourceWithExternalAuthorAndNoUser_shouldSuspendExternalAuthor() {
    User reporter = createUser("reporter-external-source");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-1");
    Source source = createSource("source-ext-1");
    Post post = createPost("post-ext-2", null, externalAuthor, source);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    reportService.updateReportStatus(
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
        report.getReportId());

    ExternalAuthor updatedAuthor =
        externalAuthorRepository.findById(externalAuthor.getExternalAuthorId()).orElseThrow();
    assertTrue(updatedAuthor.isSuspended());
  }

  @Test
  void
      updateReportStatus_withReportedSourceWithExternalAuthorAndNoUserInvalidPostID_shouldThrowException() {
    User reporter = createUser("reporter-external-source");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-1");
    Source source = createSource("source-ext-1");
    Post post = createPost("post-ext-2", null, externalAuthor, source);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.POST,
            "invlaid_id",
            ReportStatus.CREATED,
            null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
                report.getReportId()));
  }

  @Test
  void
      updateReportStatus_withReportedSourceWithExternalAuthorAndNoUserInvalidSourceID_shouldThrowException() {
    User reporter = createUser("reporter-external-source");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-1");
    Source source = createSource("source-ext-1");
    Post post = createPost("post-ext-2", null, externalAuthor, source);

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            "invalid_id",
            ReportStatus.CREATED,
            null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
                report.getReportId()));
  }

  @Test
  void
      updateReportStatus_withReportedSourceWithExternalAuthorAndNoUserSourceWithNoPost_shouldThrowException() {
    User reporter = createUser("reporter-external-source");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-1");
    Source source = createSource("source-ext-1");

    Report report =
        createReport(
            reporter,
            null,
            externalAuthor,
            ContentTypeEnum.SOURCE,
            source.getSourceId(),
            ReportStatus.CREATED,
            null);

    assertThrows(
        EntityNotFoundException.class,
        () ->
            reportService.updateReportStatus(
                new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null),
                report.getReportId()));
  }

  @Test
  void makeReport_withSourceFromExternalUser_ShouldCreateReport() {
    User reporter = createUser("reporter-external-source-report");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-report-1");
    Source source = createSource("source-report-1");
    createPost("post-report-1", null, externalAuthor, source);

    CreateReportRequest request =
        new CreateReportRequest(
            "spam",
            "bad content",
            source.getSourceId(),
            ContentTypeEnum.SOURCE,
            reporter.getUsername());

    reportService.makeReport(request, reporter);

    Report savedReport = reportRepository.findAll().get(0);
    assertEquals(ReportStatus.CREATED, savedReport.getStatus());
    assertEquals(ContentTypeEnum.SOURCE, savedReport.getContentType());
    assertEquals(source.getSourceId(), savedReport.getContentId());
    assertNotNull(savedReport.getExternalAuthorReported());
    assertEquals(
        externalAuthor.getExternalAuthorId(),
        savedReport.getExternalAuthorReported().getExternalAuthorId());
    assertNotNull(savedReport.getCreatedAt());
  }

  @Test
  void makeReport_withSourceFromExternalUserAndInvalidPostId_ShouldThrowException() {
    User reporter = createUser("reporter-external-source-report");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-report-1");

    CreateReportRequest request =
        new CreateReportRequest(
            "spam", "bad content", "wrongID", ContentTypeEnum.POST, reporter.getUsername());

    assertThrows(EntityNotFoundException.class, () -> reportService.makeReport(request, reporter));
  }

  @Test
  void makeReport_withSourceFromExternalUserAndSourceWithoutPost_ShouldThrowException() {
    User reporter = createUser("reporter-external-source-report");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-report-1");
    Source source = createSource("source-ext-1");

    CreateReportRequest request =
        new CreateReportRequest(
            "spam",
            "bad content",
            source.getSourceId(),
            ContentTypeEnum.SOURCE,
            externalAuthor.getDisplayName());

    assertThrows(EntityNotFoundException.class, () -> reportService.makeReport(request, reporter));
  }

  @Test
  void makeReport_withSourceFromExternalUserAndInvalidType_ShouldThrowException() {
    User reporter = createUser("reporter-external-source-report");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-report-1");
    Source source = createSource("source-ext-1");

    CreateReportRequest request =
        new CreateReportRequest(
            "spam",
            "bad content",
            source.getSourceId(),
            ContentTypeEnum.COMMENT,
            externalAuthor.getDisplayName());

    assertThrows(IllegalArgumentException.class, () -> reportService.makeReport(request, reporter));
  }

  @Test
  void getReportedSources_whenReportedUserMissing_usesExternalAuthorDisplayName() {
    User reporter = createUser("reporter-source-user-missing");
    ExternalAuthor externalAuthor = createExternalAuthor("ext-source-user-missing");
    Source source = createSource("source-user-missing-1");
    createPost("post-user-missing-1", null, externalAuthor, source);

    createReport(
        reporter,
        null,
        null,
        ContentTypeEnum.SOURCE,
        source.getSourceId(),
        ReportStatus.CREATED,
        null);

    var responses = reportService.getReportedSources(ReportStatus.CREATED, 0, 10);

    assertEquals(1, responses.size());
    assertEquals(externalAuthor.getDisplayName(), responses.get(0).reportedUsername());
  }

  @Test
  void getReportedSources_whenReportedUserMissingAndNoPost_throwsException() {
    User reporter = createUser("reporter-source-no-post");
    Source source = createSource("source-no-post-1");

    createReport(
        reporter,
        null,
        null,
        ContentTypeEnum.SOURCE,
        source.getSourceId(),
        ReportStatus.CREATED,
        null);

    assertThrows(
        EntityNotFoundException.class,
        () -> reportService.getReportedSources(ReportStatus.CREATED, 0, 10));
  }

  @Test
  void getAccountReports_returnsReportedAccountWithReporterAndUser() {
    User reporter = createUser("account-reporter");
    User reportedUser = createUser("account-reported");

    Report report =
        createReport(
            reporter,
            reportedUser,
            null,
            ContentTypeEnum.ACCOUNT,
            reportedUser.getUsername(),
            ReportStatus.CREATED,
            LocalDateTime.now());

    var responses = reportService.getAccountReports(ReportStatus.CREATED, 0, 10);

    assertEquals(1, responses.size());
    var response = responses.get(0);
    assertEquals(report.getReportId(), response.reportId());
    assertEquals("spam", response.reportReason());
    assertEquals("description", response.reportDescription());
    assertEquals(reporter.getUsername(), response.reportedByUsername());
    assertEquals(reportedUser.getUsername(), response.reportedUsername());
    assertEquals(ContentTypeEnum.ACCOUNT, response.contentType());
    assertEquals(ReportStatus.CREATED, response.status());
    assertNotNull(response.profileResponse());
    assertEquals(reportedUser.getUsername(), response.profileResponse().username());
  }

  @Test
  void getAccountReports_filtersOutReportsWithMissingReportedUser() {
    createReport(
        createUser("account-reporter-null-user"),
        null,
        null,
        ContentTypeEnum.ACCOUNT,
        "missing-user",
        ReportStatus.CREATED,
        LocalDateTime.now());

    var responses = reportService.getAccountReports(ReportStatus.CREATED, 0, 10);

    assertTrue(responses.isEmpty());
  }

  @Test
  void toReportedSourceResponse_withInvalidContentId_shouldThrowException() {
    User reporter = createUser("reporter-invalid-content-id");
    User reportedUser = createUser("reported-invalid-content-id");
    Report report =
        Report.builder()
            .fromUser(reporter)
            .usernameReported(reportedUser)
            .externalAuthorReported(null)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId("invalid-id")
            .reason("spam")
            .description("description")
            .status(ReportStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .build();

    assertThrows(
        EntityNotFoundException.class, () -> reportMapper.toReportedSourceResponse(report));
  }

  private User createUser(String username) {
    User user = new User();
    user.setUsername(username);
    user.setPassword("password");
    user.setEmail(username + "@example.com");
    user.setEmailVerified(true);
    user.setBirthdate(LocalDate.of(1990, 1, 1));
    return userRepository.save(user);
  }

  private ExternalAuthor createExternalAuthor(String id) {
    ExternalAuthor author =
        ExternalAuthor.builder()
            .externalAuthorId(id)
            .source("mastodon")
            .displayName("Author " + id)
            .avatarUrl("avatar")
            .profileUrl("profile")
            .build();
    return externalAuthorRepository.save(author);
  }

  private Source createSource(String id) {
    Source source =
        Source.builder()
            .sourceId(id)
            .upvotes(0)
            .downvotes(0)
            .isRemoved(false)
            .createdAt(LocalDateTime.now())
            .build();

    SingleSource singleSource =
        SingleSource.builder()
            .singleSourceId("single-" + id)
            .title("Title")
            .url("https://example.com")
            .source(source)
            .build();

    List<SingleSource> singleSources = new ArrayList<>();
    singleSources.add(singleSource);
    source.setSingleSources(singleSources);

    return sourceRepository.save(source);
  }

  private Post createPost(String id, User user, ExternalAuthor author, Source source) {
    Post post =
        Post.builder()
            .postId(id)
            .user(user)
            .externalAuthor(author)
            .source(source)
            .caption("caption")
            .isPublic(true)
            .upvotes(0)
            .downvotes(0)
            .createdAt(LocalDateTime.now())
            .build();
    return postRepository.save(post);
  }

  private Comment createComment(String id, Post post, User user) {
    Comment comment =
        Comment.builder()
            .commentId(id)
            .post(post)
            .user(user)
            .content("comment content")
            .createdAt(LocalDateTime.now())
            .isRemoved(false)
            .build();
    return commentRepository.save(comment);
  }

  private Report createReport(
      User reporter,
      User reportedUser,
      ExternalAuthor externalAuthor,
      ContentTypeEnum contentType,
      String contentId,
      ReportStatus status,
      LocalDateTime createdAt) {
    Report report =
        Report.builder()
            .fromUser(reporter)
            .usernameReported(reportedUser)
            .externalAuthorReported(externalAuthor)
            .contentType(contentType)
            .contentId(contentId)
            .reason("spam")
            .description("description")
            .status(status)
            .createdAt(createdAt)
            .build();
    return reportRepository.save(report);
  }
}
