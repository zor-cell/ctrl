package at.ac.tuwien.inso.ctrl.integrationTests;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.ac.tuwien.inso.ctrl.baseTest.TestConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Import(TestConfig.class)
@ActiveProfiles("test")
class WebSocketSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void wsInfo_isPublic() throws Exception {
    mockMvc.perform(get("/ws/info").queryParam("t", "1")).andExpect(status().isOk());
  }
}
