package at.ac.tuwien.inso.ctrl.algorithm.config;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Configuration
public class TopicSeeder {

    @Bean
    CommandLineRunner seedTopics(TopicRepository topicRepository) {
        return args -> {
            /*var now = LocalDateTime.now();
            var defaultTopics = List.of(
                    new DefaultTopic("trusted", "✅"),
                    new DefaultTopic("unverified", "❓"),
                    new DefaultTopic("disputed", "⚠️"),
                    new DefaultTopic("popular", "🔥"),
                    new DefaultTopic("highly discussed", "💬"),
                    new DefaultTopic("reshared", "🔁"),
                    new DefaultTopic("recent", "🕒"),
                    new DefaultTopic("old post", "🗂️"),
                    new DefaultTopic("from followed creator", "❤️"),
                    new DefaultTopic("AI generated content", "🤖")
            );

            var topicsToCreate = defaultTopics.stream()
                    .filter(topic -> !topicRepository.existsByName(topic.name()))
                    .map(topic -> Topic.builder()
                            .topicId(toTopicId(topic.name()))
                            .name(topic.name())
                            .emoji(topic.emoji())
                            .createdAt(now)
                            .build())
                    .toList();

            if (!topicsToCreate.isEmpty()) {
                topicRepository.saveAll(topicsToCreate);
            }*/
        };
    }

    private String toTopicId(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replace(" ", "_");
    }

    private record DefaultTopic(String name, String emoji) {
    }
}
