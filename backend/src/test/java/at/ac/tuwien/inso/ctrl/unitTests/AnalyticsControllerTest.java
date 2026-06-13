package at.ac.tuwien.inso.ctrl.unitTests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import at.ac.tuwien.inso.ctrl.session.dto.AverageDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.AverageDayResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayDetailResponse;
import at.ac.tuwien.inso.ctrl.session.dto.LastWeekDayResponse;
import at.ac.tuwien.inso.ctrl.session.services.AnalyticsService;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.List;
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
import org.springframework.test.web.servlet.MvcResult;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(TestConfig.class)
@AutoConfigureMockMvc
public class AnalyticsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private AnalyticsService analyticsService;

  private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

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
  class GetWeek {

    @Test
    void shouldReturnHttp200_whenUserIsAuthenticated() throws Exception {
      User user = authenticatedUser();
      when(analyticsService.getWeek(user, 0)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/users/me/analytics/week/0").with(user(user)))
          .andExpect(status().isOk());
    }

    @Test
    void shouldReturnResponseBodyMatchingListOfLastWeekDayResponse_whenUserIsAuthenticated()
        throws Exception {
      User user = authenticatedUser();
      List<LastWeekDayResponse> expected =
          List.of(new LastWeekDayResponse(java.time.LocalDate.now(), 0L, 0L));
      when(analyticsService.getWeek(user, 0)).thenReturn(expected);

      MvcResult result =
          mockMvc
              .perform(get("/api/users/me/analytics/week/0").with(user(user)))
              .andExpect(status().isOk())
              .andReturn();

      List<LastWeekDayResponse> body =
          objectMapper.readValue(
              result.getResponse().getContentAsString(),
              new TypeReference<List<LastWeekDayResponse>>() {});
      assertThat(body).isEqualTo(expected);
    }
  }

  @Nested
  class GetWeekDetailed {

    @Test
    void shouldReturnHttp200_whenUserIsAuthenticated() throws Exception {
      User user = authenticatedUser();
      when(analyticsService.getWeekDetails(user, 0)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/users/me/analytics/week/detailed/0").with(user(user)))
          .andExpect(status().isOk());
    }

    @Test
    void shouldReturnResponseBodyMatchingListOfLastWeekDayDetailResponse_whenUserIsAuthenticated()
        throws Exception {
      User user = authenticatedUser();
      List<LastWeekDayDetailResponse> expected = List.of();
      when(analyticsService.getWeekDetails(user, 0)).thenReturn(expected);

      MvcResult result =
          mockMvc
              .perform(get("/api/users/me/analytics/week/detailed/0").with(user(user)))
              .andExpect(status().isOk())
              .andReturn();

      List<LastWeekDayDetailResponse> body =
          objectMapper.readValue(
              result.getResponse().getContentAsString(),
              new TypeReference<List<LastWeekDayDetailResponse>>() {});
      assertThat(body).isEqualTo(expected);
    }
  }

  @Nested
  class GetWeeklyAverage {

    @Test
    void shouldReturnHttp200_whenUserIsAuthenticated() throws Exception {
      User user = authenticatedUser();
      when(analyticsService.getAverageWeek(user)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/users/me/analytics/weekly-average").with(user(user)))
          .andExpect(status().isOk());
    }

    @Test
    void shouldReturnResponseBodyMatchingListOfAverageDayResponse_whenUserIsAuthenticated()
        throws Exception {
      User user = authenticatedUser();
      List<AverageDayResponse> expected = List.of(new AverageDayResponse(0, 0.0, 0.0, 0));
      when(analyticsService.getAverageWeek(user)).thenReturn(expected);

      MvcResult result =
          mockMvc
              .perform(get("/api/users/me/analytics/weekly-average").with(user(user)))
              .andExpect(status().isOk())
              .andReturn();

      List<AverageDayResponse> body =
          objectMapper.readValue(
              result.getResponse().getContentAsString(),
              new TypeReference<List<AverageDayResponse>>() {});
      assertThat(body).isEqualTo(expected);
    }
  }

  @Nested
  class GetWeeklyAverageDetailed {

    @Test
    void shouldReturnHttp200_whenUserIsAuthenticated() throws Exception {
      User user = authenticatedUser();
      when(analyticsService.getAverageWeekDetails(user)).thenReturn(List.of());

      mockMvc
          .perform(get("/api/users/me/analytics/weekly-average/detailed").with(user(user)))
          .andExpect(status().isOk());
    }

    @Test
    void shouldReturnResponseBodyMatchingListOfAverageDayDetailResponse_whenUserIsAuthenticated()
        throws Exception {
      User user = authenticatedUser();
      List<AverageDayDetailResponse> expected =
          List.of(new AverageDayDetailResponse("", 0, List.of(), 0));
      when(analyticsService.getAverageWeekDetails(user)).thenReturn(expected);

      MvcResult result =
          mockMvc
              .perform(get("/api/users/me/analytics/weekly-average/detailed").with(user(user)))
              .andExpect(status().isOk())
              .andReturn();

      List<AverageDayDetailResponse> body =
          objectMapper.readValue(
              result.getResponse().getContentAsString(),
              new TypeReference<List<AverageDayDetailResponse>>() {});
      assertThat(body).isEqualTo(expected);
    }
  }
}
