package at.ac.tuwien.inso.ctrl.algorithm.config;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TopicSeeder {
  private final EmbeddingService embeddingService;

  @Bean
  CommandLineRunner seedTopics(TopicRepository topicRepository) {
    return args -> {
      var defaultTopics =
          List.of(
              new DefaultTopic("Health", "🩺"),
              new DefaultTopic("Economics", "📈"),
              new DefaultTopic("Politics", "🏛️"),
              new DefaultTopic("Science", "🔬"),
              new DefaultTopic("Technology", "💻"),
              new DefaultTopic("Environment", "🌱"),
              new DefaultTopic("Education", "🎓"),
              new DefaultTopic("Culture", "🎭"),
              new DefaultTopic("Sports", "🏅"),
              new DefaultTopic("Society", "🤝"));

      var topicsToCreate =
          defaultTopics.stream()
              .filter(topic -> !topicRepository.existsByName(topic.name()))
              .map(
                  topic ->
                      Topic.builder()
                          .name(topic.name())
                          .emoji(topic.emoji())
                              .embedding(embeddingService.embedText(topic.name()))
                          .build())
              .toList();

      if (!topicsToCreate.isEmpty()) {
        topicRepository.saveAll(topicsToCreate);
      }
    };
  }

  private record DefaultTopic(String name, String emoji) {}
}
