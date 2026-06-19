package at.ac.tuwien.inso.ctrl.baseTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import com.google.api.services.gmail.Gmail;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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

  @Bean
  public EmbeddingService embeddingService() {
    EmbeddingService mockService = Mockito.mock(EmbeddingService.class);
    float[] embedding = new float[EmbeddingModelConfig.MODEL_DIMENSIONS];
    Arrays.fill(embedding, 1.0f);
    when(mockService.embedText(any(String.class))).thenReturn(embedding);
    return mockService;
  }

  public static final Clock FIXED_CLOCK =
      Clock.fixed(LocalDateTime.of(2026, 5, 1, 12, 0).toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

  @Bean
  @Primary
  Clock clock() {
    return FIXED_CLOCK;
  }
}
