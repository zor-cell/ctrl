package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
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
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.report.service.ReportService;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM),
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
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM),
        report.getReportId());

    Source updatedSource = sourceRepository.findById(source.getSourceId()).orElseThrow();
    assertTrue(updatedSource.getIsRemoved());
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
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT),
        report.getReportId());

    User updatedUser = userRepository.findById(contentUser.getId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
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
