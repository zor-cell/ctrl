package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.auth.service.GmailEmailService;
import at.ac.tuwien.inso.ctrl.feed.mapper.FeedMapper;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostVoteRepository;
import at.ac.tuwien.inso.ctrl.report.mapper.ReportMapper;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.report.service.ReportServiceImp;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;

@ExtendWith(MockitoExtension.class)
public class ReportServiceUnitTest {
  @Mock private ReportRepository reportRepository;
  @Mock private PostRepository postRepository;
  @Mock private SourceRepository sourceRepository;
  @Mock private PostVoteRepository postVoteRepository;
  @Mock private SourceVoteRepository sourceVoteRepository;
  @Mock private UserRepository userRepository;
  @Mock private FeedMapper feedMapper;
  @Mock private ExternalAuthorRepository externalAuthorRepository;
  @Mock private GmailEmailService gmailEmailService;
  @Mock private TemplateEngine templateEngine;

  @InjectMocks private ReportServiceImp reportService;
  @InjectMocks private ReportMapper reportMapper;

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
}
