package at.ac.tuwien.inso.ctrl.algorithm.config;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicSeeder {

  @Bean
  CommandLineRunner seedTopics(TopicRepository topicRepository) {
    return args -> {
      var now = LocalDateTime.now();
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
              .filter(topic -> !topicRepository.existsById(toTopicId(topic.name())))
              .map(
                  topic ->
                      Topic.builder()
                          .topicId(toTopicId(topic.name()))
                          .name(topic.name())
                          .emoji(topic.emoji())
                          .createdAt(now)
                          .build())
              .toList();

      if (!topicsToCreate.isEmpty()) {
        topicRepository.saveAll(topicsToCreate);
      }
    };
  }

  private String toTopicId(String name) {
    return name.toLowerCase(Locale.ROOT).replace(" ", "_");
  }

  private record DefaultTopic(String name, String emoji) {}
}
