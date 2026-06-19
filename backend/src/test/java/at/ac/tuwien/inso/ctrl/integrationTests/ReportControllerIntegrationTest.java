package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.comments.repository.CommentRepository;
import at.ac.tuwien.inso.ctrl.config.security.JwtService;
import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.dto.ReportAction;
import at.ac.tuwien.inso.ctrl.report.dto.UpdateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.entity.Report;
import at.ac.tuwien.inso.ctrl.report.entity.ReportStatus;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.service.IdAustriaOidcService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class ReportControllerIntegrationTest {
  @Autowired private MockMvc mockMvc;

  @Autowired private UserRepository userRepository;
  @Autowired private PostRepository postRepository;
  @Autowired private ReportRepository reportRepository;
  @Autowired private SourceRepository sourceRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtService jwtService;

  @Autowired private CommentRepository commentRepository;

  @MockitoBean private IdAustriaOidcService idAustriaOidcService;
  @MockitoBean private GmailEmailService gmailEmailService;
  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  private final String reportEndpoint = "/api/report";
  private User adminUser;
  private String adminJwtToken;
  private String standardUserJwtToken;
  private User contentUser;
  private Post post;
  private Source source;
  private Comment comment1;
  private Comment comment2;

  @BeforeEach
  public void setUp() {
    // Clean up all data
    reportRepository.deleteAll();
    commentRepository.deleteAll();
    postRepository.deleteAll();
    sourceRepository.deleteAll();
    userRepository.deleteAll();

    adminUser =
        User.builder()
            .username("adminUser")
            .password(passwordEncoder.encode("pass"))
            .email("admin@test.com")
            .role(Role.ADMIN)
            .isVerified(true)
            .build();
    userRepository.save(adminUser);
    adminJwtToken = jwtService.generateToken(adminUser);

    User standardUser =
        User.builder()
            .username("standardUser")
            .password(passwordEncoder.encode("pass"))
            .email("standard@test.com")
            .role(Role.USER)
            .isVerified(true)
            .build();
    userRepository.save(standardUser);
    standardUserJwtToken = jwtService.generateToken(standardUser);

    contentUser =
        User.builder()
            .username("contentUser")
            .password(passwordEncoder.encode("pass"))
            .email("content@test.com")
            .role(Role.USER)
            .isVerified(true)
            .build();
    userRepository.save(contentUser);

    source = Source.builder().sourceId("source-1").singleSources(List.of()).build();

    sourceRepository.save(source);

    post =
        Post.builder()
            .postId("post-1")
            .user(contentUser)
            .caption("caption")
            .isPublic(true)
            .upvotes(0)
            .downvotes(0)
            .source(source)
            .createdAt(LocalDateTime.now())
            .build();
    postRepository.save(post);

    // Create comments
    comment1 =
        Comment.builder()
            .commentId("comment-1")
            .user(contentUser)
            .post(post)
            .content("This is a comment.")
            .parentComment(null)
            .replies(List.of())
            .createdAt(LocalDateTime.now())
            .build();

    comment2 =
        Comment.builder()
            .commentId("comment-2")
            .user(contentUser)
            .post(post)
            .content("This is another comment.")
            .parentComment(null)
            .replies(List.of())
            .createdAt(LocalDateTime.now())
            .build();

    commentRepository.saveAll(List.of(comment1, comment2));
  }

  @AfterEach
  public void tearDown() {
    reportRepository.deleteAll();
    commentRepository.deleteAll();
    postRepository.deleteAll();
    sourceRepository.deleteAll();
    userRepository.deleteAll();
  }

  @Test
  public void makeReport_shouldPersistReportOfPost_WhenGivenReportVerification() throws Exception {

    CreateReportRequest request =
        new CreateReportRequest(
            "spam", "bad content", post.getPostId(), ContentTypeEnum.POST, adminUser.getUsername());

    mockMvc
        .perform(
            post(reportEndpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    List<Report> reports = reportRepository.findAll();
    assertEquals(1, reports.size());

    Report savedReport = reports.getFirst();
    assertEquals(post.getPostId(), savedReport.getContentId());
    assertEquals(ContentTypeEnum.POST, savedReport.getContentType());
    assertEquals(ReportStatus.CREATED, savedReport.getStatus());
    assertNotNull(savedReport.getCreatedAt());
    assertEquals(adminUser.getId(), savedReport.getFromUser().getId());
    assertEquals(contentUser.getId(), savedReport.getUsernameReported().getId());
  }

  @Test
  public void getReportedSourcesWithStatusCreated_shouldReturnNoReports_WhenCalled()
      throws Exception {

    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  public void getReportedSourcesWithStatusCreated_shouldReturnOneReport_WhenCalled()
      throws Exception {

    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source.getSourceId())
            .reason("Inappropriate content")
            .description("This source contains inappropriate content.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].contentType").value("SOURCE"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportReason").value("Inappropriate content"))
        .andExpect(
            jsonPath("$[0].reportDescription").value("This source contains inappropriate content."))
        .andExpect(jsonPath("$[0].sourceResponse.sourceId").value(source.getSourceId()))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()));
  }

  @Test
  public void getReportedPostsWithStatusCreated_shouldReturnOneReport_WhenCalled()
      throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Spam")
            .description("This post is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/posts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].contentType").value("POST"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportReason").value("Spam"))
        .andExpect(jsonPath("$[0].reportDescription").value("This post is spam."))
        .andExpect(jsonPath("$[0].postId").value(post.getPostId()))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()));
  }

  @Test
  public void getReportedPostsWithStatusCreated_shouldReturnNoReport_WhenCalled() throws Exception {

    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  public void getAccountReportSummary_shouldReturnEmpty_WhenNoValidatedReports() throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Spam")
            .description("This post is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/" + report.getReportId() + "/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  public void getAccountReportSummary_shouldReturnSummary_WhenValidatedReportsExist()
      throws Exception {
    Report firstReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Spam")
            .description("Repeated spam content.")
            .fromUser(adminUser)
            .status(ReportStatus.VALIDATED)
            .build();
    Report secondReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Abuse")
            .description("Harassment in comments.")
            .fromUser(adminUser)
            .status(ReportStatus.VALIDATED)
            .build();
    reportRepository.save(firstReport);
    reportRepository.save(secondReport);

    mockMvc
        .perform(
            get(reportEndpoint + "/" + firstReport.getReportId() + "/summary")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(
            jsonPath(
                "$[*].contentType",
                org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("POST"))))
        .andExpect(
            jsonPath("$[*].reason", org.hamcrest.Matchers.containsInAnyOrder("Spam", "Abuse")))
        .andExpect(
            jsonPath(
                "$[*].description",
                org.hamcrest.Matchers.containsInAnyOrder(
                    "Repeated spam content.", "Harassment in comments.")));
  }

  @Test
  public void updateReport_shouldDeleteItemAndValidate_WhenPostReported() throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Spam")
            .description("This post is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .createdAt(null)
            .build();
    reportRepository.save(report);

    UpdateReportRequest request =
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ITEM, null);

    mockMvc
        .perform(
            put(reportEndpoint + "/" + report.getReportId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    Post updatedPost = postRepository.findById(post.getPostId()).orElseThrow();
    Report updatedReport = reportRepository.findById(report.getReportId()).orElseThrow();
    assertEquals(true, updatedPost.getIsRemoved());
    assertEquals(ReportStatus.VALIDATED, updatedReport.getStatus());
    assertNotNull(updatedReport.getCreatedAt());
  }

  @Test
  public void updateReport_shouldSuspendAccount_WhenDeleteAccountAction() throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.POST)
            .contentId(post.getPostId())
            .reason("Spam")
            .description("This post is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .createdAt(null)
            .build();
    reportRepository.save(report);

    UpdateReportRequest request =
        new UpdateReportRequest(ReportStatus.VALIDATED, ReportAction.DELETE_ACCOUNT, null);

    mockMvc
        .perform(
            put(reportEndpoint + "/" + report.getReportId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());

    User updatedUser = userRepository.findById(contentUser.getId()).orElseThrow();
    Report updatedReport = reportRepository.findById(report.getReportId()).orElseThrow();
    assertTrue(updatedUser.isSuspended());
    assertTrue(updatedUser.isDeleted());
    assertEquals("Deleted user", updatedUser.getName());
    assertEquals(ReportStatus.VALIDATED, updatedReport.getStatus());
    assertNotNull(updatedReport.getCreatedAt());
  }

  @Test
  public void getReportedSources_shouldReturnForbidden_WhenCalledByStandardUser() throws Exception {
    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + standardUserJwtToken))
        .andExpect(status().isForbidden());
  }

  @Test
  public void getReportedSources_shouldReturnMultipleReports_WhenMultipleSourcesReported()
      throws Exception {
    Report report1 =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source.getSourceId())
            .reason("Inappropriate content")
            .description("This source contains inappropriate content.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    Source source2 = Source.builder().sourceId("source-2").singleSources(List.of()).build();
    sourceRepository.save(source2);

    Report report2 =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source2.getSourceId())
            .reason("Misinformation")
            .description("This source spreads misinformation.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    reportRepository.saveAll(List.of(report1, report2));

    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].reportReason").value("Inappropriate content"))
        .andExpect(jsonPath("$[1].reportReason").value("Misinformation"));
  }

  @Test
  public void getReportedSources_shouldFilterByStatus_WhenStatusParameterProvided()
      throws Exception {
    Report createdReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source.getSourceId())
            .reason("Inappropriate content")
            .description("This source contains inappropriate content.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    Source source2 = Source.builder().sourceId("source-2").singleSources(List.of()).build();
    sourceRepository.save(source2);

    Report validatedReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source2.getSourceId())
            .reason("Misinformation")
            .description("This source spreads misinformation.")
            .fromUser(adminUser)
            .status(ReportStatus.VALIDATED)
            .build();

    reportRepository.saveAll(List.of(createdReport, validatedReport));

    mockMvc
        .perform(
            get(reportEndpoint + "/sources?status=VALIDATED")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].status").value("VALIDATED"))
        .andExpect(jsonPath("$[0].reportReason").value("Misinformation"));
  }

  @Test
  public void getReportedSources_shouldReturnPaginatedResults_WhenPageParameterProvided()
      throws Exception {
    for (int i = 1; i <= 5; i++) {
      Source src = Source.builder().sourceId("source-" + i).singleSources(List.of()).build();
      sourceRepository.save(src);

      Report report =
          Report.builder()
              .usernameReported(contentUser)
              .contentType(ContentTypeEnum.SOURCE)
              .contentId(src.getSourceId())
              .reason("Reason " + i)
              .description("Description " + i)
              .fromUser(adminUser)
              .status(ReportStatus.CREATED)
              .build();
      reportRepository.save(report);
    }

    // Get first page with 2 items per page
    mockMvc
        .perform(
            get(reportEndpoint + "/sources?page=0&postsPerPage=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

    // Get second page with 2 items per page
    mockMvc
        .perform(
            get(reportEndpoint + "/sources?page=1&postsPerPage=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
  }

  @Test
  public void getReportedSources_shouldReturnReportWithCorrectFields_WhenSourceReported()
      throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.SOURCE)
            .contentId(source.getSourceId())
            .reason("Inappropriate content")
            .description("This source contains inappropriate content.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/sources")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reportId").value(report.getReportId()))
        .andExpect(jsonPath("$[0].reportReason").value("Inappropriate content"))
        .andExpect(
            jsonPath("$[0].reportDescription").value("This source contains inappropriate content."))
        .andExpect(jsonPath("$[0].contentType").value("SOURCE"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()))
        .andExpect(jsonPath("$[0].sourceResponse").exists());
  }

  // ========== Tests for /comments endpoint ==========

  @Test
  public void getReportedComments_shouldReturnForbidden_WhenCalledByStandardUser()
      throws Exception {
    mockMvc
        .perform(
            get(reportEndpoint + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + standardUserJwtToken))
        .andExpect(status().isForbidden());
  }

  @Test
  public void getReportedComments_shouldReturnNoReports_WhenNoneExist() throws Exception {
    mockMvc
        .perform(
            get(reportEndpoint + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
  }

  @Test
  public void getReportedComments_shouldReturnOneReport_WhenCommentReported() throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment1.getCommentId())
            .reason("Inappropriate language")
            .description("This comment contains inappropriate language.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].contentType").value("COMMENT"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportReason").value("Inappropriate language"))
        .andExpect(
            jsonPath("$[0].reportDescription")
                .value("This comment contains inappropriate language."))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()));
  }

  @Test
  public void getReportedComments_shouldReturnMultipleReports_WhenMultipleCommentsReported()
      throws Exception {
    Report report1 =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment1.getCommentId())
            .reason("Inappropriate language")
            .description("This comment contains inappropriate language.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    Report report2 =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment2.getCommentId())
            .reason("Spam")
            .description("This comment is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    reportRepository.saveAll(List.of(report1, report2));

    mockMvc
        .perform(
            get(reportEndpoint + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
        .andExpect(jsonPath("$[0].reportReason").value("Inappropriate language"))
        .andExpect(jsonPath("$[1].reportReason").value("Spam"));
  }

  @Test
  public void getReportedComments_shouldFilterByStatus_WhenStatusParameterProvided()
      throws Exception {
    Report createdReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment1.getCommentId())
            .reason("Inappropriate language")
            .description("This comment contains inappropriate language.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .build();

    Report validatedReport =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment2.getCommentId())
            .reason("Spam")
            .description("This comment is spam.")
            .fromUser(adminUser)
            .status(ReportStatus.VALIDATED)
            .build();

    reportRepository.saveAll(List.of(createdReport, validatedReport));

    mockMvc
        .perform(
            get(reportEndpoint + "/comments?status=VALIDATED")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
        .andExpect(jsonPath("$[0].status").value("VALIDATED"))
        .andExpect(jsonPath("$[0].reportReason").value("Spam"));
  }

  @Test
  public void getReportedComments_shouldReturnPaginatedResults_WhenPageParameterProvided()
      throws Exception {
    // Create multiple comments for pagination testing
    for (int i = 3; i <= 7; i++) {
      Comment comment =
          Comment.builder()
              .commentId("comment-" + i)
              .user(contentUser)
              .post(post)
              .content("This is comment " + i)
              .parentComment(null)
              .replies(List.of())
              .createdAt(LocalDateTime.now())
              .build();
      commentRepository.save(comment);

      Report report =
          Report.builder()
              .usernameReported(contentUser)
              .contentType(ContentTypeEnum.COMMENT)
              .contentId(comment.getCommentId())
              .reason("Reason " + i)
              .description("Description " + i)
              .fromUser(adminUser)
              .status(ReportStatus.CREATED)
              .build();
      reportRepository.save(report);
    }

    // Get first page with 2 items per page
    mockMvc
        .perform(
            get(reportEndpoint + "/comments?page=0&postsPerPage=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));

    // Get second page with 2 items per page
    mockMvc
        .perform(
            get(reportEndpoint + "/comments?page=1&postsPerPage=2")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)));
  }

  @Test
  public void getReportedComments_shouldReturnReportWithCorrectFields_WhenCommentReported()
      throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.COMMENT)
            .contentId(comment1.getCommentId())
            .reason("Harassment")
            .description("This comment is harassing other users.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/comments")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reportId").value(report.getReportId()))
        .andExpect(jsonPath("$[0].reportReason").value("Harassment"))
        .andExpect(
            jsonPath("$[0].reportDescription").value("This comment is harassing other users."))
        .andExpect(jsonPath("$[0].contentType").value("COMMENT"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()))
        .andExpect(jsonPath("$[0].comment").exists());
  }

  @Test
  public void getReportedComments_shouldReturnUnauthorized_WhenNoAuthorizationToken()
      throws Exception {
    mockMvc.perform(get(reportEndpoint + "/comments")).andExpect(status().isUnauthorized());
  }

  @Test
  public void getReportedSources_shouldReturnUnauthorized_WhenNoAuthorizationToken()
      throws Exception {
    mockMvc.perform(get(reportEndpoint + "/sources")).andExpect(status().isUnauthorized());
  }

  @Test
  public void getReportedAccounts_shouldReturnUnauthorized_WhenNoAuthorizationToken()
      throws Exception {
    mockMvc.perform(get(reportEndpoint + "/accounts")).andExpect(status().isUnauthorized());
  }

  @Test
  public void getReportedAccounts_shouldReturnReportWithCorrectFields_WhenCommentReported()
      throws Exception {
    Report report =
        Report.builder()
            .usernameReported(contentUser)
            .contentType(ContentTypeEnum.ACCOUNT)
            .contentId(String.valueOf(contentUser.getId()))
            .reason("Harassment")
            .description("This comment is harassing other users.")
            .fromUser(adminUser)
            .status(ReportStatus.CREATED)
            .createdAt(LocalDateTime.now())
            .build();
    reportRepository.save(report);

    mockMvc
        .perform(
            get(reportEndpoint + "/accounts")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].reportId").value(report.getReportId()))
        .andExpect(jsonPath("$[0].reportReason").value("Harassment"))
        .andExpect(
            jsonPath("$[0].reportDescription").value("This comment is harassing other users."))
        .andExpect(jsonPath("$[0].contentType").value("ACCOUNT"))
        .andExpect(jsonPath("$[0].status").value("CREATED"))
        .andExpect(jsonPath("$[0].reportedByUsername").value(adminUser.getUsername()))
        .andExpect(jsonPath("$[0].reportedUsername").value(contentUser.getUsername()))
        .andExpect(jsonPath("$[0].profileResponse").exists());
  }
}
