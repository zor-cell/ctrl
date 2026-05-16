package at.ac.tuwien.inso.ctrl.baseTest;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import com.google.api.services.gmail.Gmail;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class TestConfig {

  @Bean
  public Gmail gmail() {
    return Mockito.mock(Gmail.class, RETURNS_DEEP_STUBS);
  }

  @Bean
  public TaskExecutor taskExecutor() {
    return new SyncTaskExecutor();
  }
}
