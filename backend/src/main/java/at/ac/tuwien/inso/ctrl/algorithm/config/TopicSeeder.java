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
            var now = LocalDateTime.now();
            var defaultTopics = List.of(
                    "trusted",
                    "unverified",
                    "disputed",
                    "popular",
                    "highly discussed",
                    "reshared",
                    "recent",
                    "old post",
                    "from followed creator",
                    "AI generated content",
            );

            var topicsToCreate = defaultTopics.stream()
                    .filter(name -> !topicRepository.existsByName(name))
                    .map(name -> Topic.builder()
                            .topicId(toTopicId(name))
                            .name(name)
                            .createdAt(now)
                            .build())
                    .toList();

            if (!topicsToCreate.isEmpty()) {
                topicRepository.saveAll(topicsToCreate);
            }
        };
    }

    private String toTopicId(String name) {
        return name.toLowerCase(Locale.ROOT)
                .replace(" ", "_");
    }
}
