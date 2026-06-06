package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.session.services.AnalyticsService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
@AutoConfigureMockMvc
public class AnalyticsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnalyticsService analyticsService;

  private User authenticatedUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .username("testuser")
        .password("password")
        .email("test@example.com")
        .name("Test User")
        .build();
  }

  @Nested
  class GetLastWeek {

    @Test
    void should_returnHttp200_whenUserIsAuthenticated() throws Exception {
      // Asserts that GET /users/me/analytics/last-week returns HTTP 200 for a valid
      // authenticated user.
    }

    @Test
    void should_returnResponseBodyMatchingLastWeekResponse_whenUserIsAuthenticated()
        throws Exception {
      // Asserts that the response body is not null and matches the LastWeekResponse
      // DTO type.
    }
  }

  @Nested
  class GetLastWeekDetailed {

    @Test
    void should_returnHttp200_whenUserIsAuthenticated() throws Exception {
      // Asserts that GET /users/me/analytics/last-week/detailed returns HTTP 200 for
      // a valid authenticated user.
    }

    @Test
    void should_returnResponseBodyMatchingLastWeekDetailResponse_whenUserIsAuthenticated()
        throws Exception {
      // Asserts that the response body is not null and matches the
      // LastWeekDetailResponse DTO type.
    }
  }

  @Nested
  class GetWeeklyAverage {

    @Test
    void should_returnHttp200_whenUserIsAuthenticated() throws Exception {
      // Asserts that GET /users/me/analytics/weekly-average returns HTTP 200 for a
      // valid authenticated user.
    }

    @Test
    void should_returnResponseBodyMatchingAverageWeekResponse_whenUserIsAuthenticated()
        throws Exception {
      // Asserts that the response body is not null and matches the
      // AverageWeekResponse DTO type.
    }
  }

  @Nested
  class GetWeeklyAverageDetailed {

    @Test
    void should_returnHttp200_whenUserIsAuthenticated() throws Exception {
      // Asserts that GET /users/me/analytics/weekly-average/detailed returns HTTP 200
      // for a valid authenticated user.
    }

    @Test
    void should_returnResponseBodyMatchingAverageWeekDetailResponse_whenUserIsAuthenticated()
        throws Exception {
      // Asserts that the response body is not null and matches the
      // AverageWeekDetailResponse DTO type.
    }
  }
}
