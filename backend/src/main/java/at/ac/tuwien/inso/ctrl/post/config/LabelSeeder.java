package at.ac.tuwien.inso.ctrl.post.config;

import at.ac.tuwien.inso.ctrl.post.entity.Label;
import at.ac.tuwien.inso.ctrl.post.repository.LabelRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LabelSeeder {

  @Bean
  CommandLineRunner seedLabels(LabelRepository labelRepository) {
    return args -> {
      List<DefaultLabel> defaultLabels =
          List.of(
              new DefaultLabel("Trusted", "✅"),
              new DefaultLabel("Unverified", "❓"),
              new DefaultLabel("Disputed", "⚠️"),
              new DefaultLabel("Popular", "🔥"),
              new DefaultLabel("Highly Discussed", "💬"),
              new DefaultLabel("Reshared", "🔁"),
              new DefaultLabel("Recent", "🕒"),
              new DefaultLabel("Old Post", "🗂️"),
              new DefaultLabel("From Followed Creator", "❤️"),
              new DefaultLabel("AI Generated Content", "🤖"));

      List<Label> labelsToCreate =
          defaultLabels.stream()
              .filter(label -> !labelRepository.existsById(toLabelId(label.name())))
              .map(
                  label ->
                      Label.builder()
                          .labelId(toLabelId(label.name()))
                          .name(label.name())
                          .emoji(label.emoji())
                          .build())
              .toList();

      if (!labelsToCreate.isEmpty()) {
        labelRepository.saveAll(labelsToCreate);
      }
    };
  }

  private String toLabelId(String name) {
    return name.toLowerCase(Locale.ROOT).replace(" ", "_");
  }

  private record DefaultLabel(String name, String emoji) {}
}
