package at.ac.tuwien.inso.ctrl.algorithm.config;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import java.util.List;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TopicSeeder {
  private final EmbeddingService embeddingService;

  @Bean
  CommandLineRunner seedTopics(TopicRepository topicRepository) {
    return args -> {
      var defaultTopics = List.of(
              // --- Content Types ---
              new DefaultTopic("News", "📰"),
              new DefaultTopic("Analysis", "📊"),
              new DefaultTopic("Discussion", "💬"),
              new DefaultTopic("Opinion", "🗣️"),
              new DefaultTopic("Debate", "⚖️"),
              new DefaultTopic("Interview", "🎙️"),
              new DefaultTopic("Review", "⭐"),
              new DefaultTopic("Guide", "🧭"),
              new DefaultTopic("Tutorial", "📘"),
              new DefaultTopic("Breakdown", "🧩"),
              new DefaultTopic("Documentary", "🎥"),
              new DefaultTopic("Highlights", "✨"),
              new DefaultTopic("Announcement", "📢"),
              new DefaultTopic("Research", "🔬"),
              new DefaultTopic("Case Study", "📑"),
              new DefaultTopic("Livestream", "🔴"),
              new DefaultTopic("Podcast", "🎧"),
              new DefaultTopic("Meme", "😂"),

              // --- SCIENCE ---
              new DefaultTopic("Science", "🔬"),
              new DefaultTopic("Physics", "⚛️"),
              new DefaultTopic("Astrophysics", "🌌"),
              new DefaultTopic("Astronomy", "🔭"),
              new DefaultTopic("Quantum Physics", "⚛️"),
              new DefaultTopic("Relativity", "🌀"),
              new DefaultTopic("Particle Physics", "⚛️"),
              new DefaultTopic("Biology", "🧬"),
              new DefaultTopic("Genetics", "🧬"),
              new DefaultTopic("Neuroscience", "🧠"),
              new DefaultTopic("Psychology", "🧠"),
              new DefaultTopic("Chemistry", "⚗️"),
              new DefaultTopic("Mathematics", "📐"),
              new DefaultTopic("Geology", "🪨"),
              new DefaultTopic("Oceanography", "🌊"),
              new DefaultTopic("Meteorology", "⛅"),
              new DefaultTopic("Botany", "🌱"),
              new DefaultTopic("Zoology", "🦁"),
              new DefaultTopic("Evolution", "🦖"),
              new DefaultTopic("Ecology", "🌍"),

              // --- SPACE ---
              new DefaultTopic("Space", "🚀"),
              new DefaultTopic("NASA", "🚀"),
              new DefaultTopic("ESA", "🛰️"),
              new DefaultTopic("Rocketry", "🚀"),
              new DefaultTopic("Mars", "🔴"),
              new DefaultTopic("Moon", "🌕"),
              new DefaultTopic("Exoplanets", "🪐"),
              new DefaultTopic("Black Holes", "🕳️"),
              new DefaultTopic("Dark Matter", "🌑"),
              new DefaultTopic("Satellites", "🛰️"),

              // --- TECHNOLOGY ---
              new DefaultTopic("Technology", "💻"),
              new DefaultTopic("Programming", "💻"),
              new DefaultTopic("Software", "🖥️"),
              new DefaultTopic("Web Development", "🌐"),
              new DefaultTopic("App Development", "📱"),
              new DefaultTopic("Open Source", "📂"),
              new DefaultTopic("Linux", "🐧"),
              new DefaultTopic("Cybersecurity", "🔐"),
              new DefaultTopic("Privacy", "🔒"),
              new DefaultTopic("Cloud Computing", "☁️"),
              new DefaultTopic("DevOps", "⚙️"),
              new DefaultTopic("Databases", "🗄️"),
              new DefaultTopic("APIs", "🔌"),
              new DefaultTopic("Networking", "🌐"),
              new DefaultTopic("Hardware", "💾"),

              // --- AI ---
              new DefaultTopic("Artificial Intelligence", "🤖"),
              new DefaultTopic("Machine Learning", "🧠"),
              new DefaultTopic("Deep Learning", "🕸️"),
              new DefaultTopic("Neural Networks", "🔗"),
              new DefaultTopic("LLM", "📚"),
              new DefaultTopic("Automation", "⚙️"),
              new DefaultTopic("AI Ethics", "⚖️"),
              new DefaultTopic("Robotics", "🤖"),
              new DefaultTopic("Computer Vision", "👁️"),
              new DefaultTopic("Natural Language Processing", "🗣️"),

              // --- BUSINESS & FINANCE ---
              new DefaultTopic("Business", "💼"),
              new DefaultTopic("Economics", "📈"),
              new DefaultTopic("Finance", "💰"),
              new DefaultTopic("Investing", "📊"),
              new DefaultTopic("Trading", "📉"),
              new DefaultTopic("Stock Market", "📈"),
              new DefaultTopic("Startups", "🚀"),
              new DefaultTopic("Entrepreneurship", "💡"),
              new DefaultTopic("Marketing", "📣"),
              new DefaultTopic("E-Commerce", "🛒"),
              new DefaultTopic("Real Estate", "🏠"),
              new DefaultTopic("Cryptocurrency", "₿"),
              new DefaultTopic("Bitcoin", "₿"),
              new DefaultTopic("Ethereum", "⟠"),
              new DefaultTopic("Blockchain", "⛓️"),

              // --- POLITICS & SOCIETY ---
              new DefaultTopic("Politics", "🏛️"),
              new DefaultTopic("Government", "🏛️"),
              new DefaultTopic("Democracy", "🗳️"),
              new DefaultTopic("Geopolitics", "🌍"),
              new DefaultTopic("Law", "⚖️"),
              new DefaultTopic("Human Rights", "✊"),
              new DefaultTopic("Education", "🎓"),
              new DefaultTopic("Society", "👥"),
              new DefaultTopic("Culture", "🏺"),
              new DefaultTopic("History", "📜"),

              // --- ENVIRONMENT ---
              new DefaultTopic("Climate", "🌍"),
              new DefaultTopic("Sustainability", "♻️"),
              new DefaultTopic("Renewable Energy", "☀️"),
              new DefaultTopic("Solar Energy", "☀️"),
              new DefaultTopic("Wind Energy", "💨"),
              new DefaultTopic("Electric Vehicles", "🚗"),
              new DefaultTopic("Conservation", "🌲"),
              new DefaultTopic("Wildlife", "🦓"),
              new DefaultTopic("Recycling", "♻️"),
              new DefaultTopic("Zero Waste", "🗑️"),

              // --- ANIMALS ---
              new DefaultTopic("Animal", "🐾"),
              new DefaultTopic("Pet", "🐶"),
              new DefaultTopic("Dog", "🐕"),
              new DefaultTopic("Cat", "🐈"),
              new DefaultTopic("Whale", "🐋"),
              new DefaultTopic("Bird", "🐦"),
              new DefaultTopic("Parrot", "🦜"),
              new DefaultTopic("Duck", "🦆"),
              new DefaultTopic("Chicken", "🐔"),
              new DefaultTopic("Penguin", "🐧"),
              new DefaultTopic("Owl", "🦉"),
              new DefaultTopic("Eagle", "🦅"),
              new DefaultTopic("Wolf", "🐺"),
              new DefaultTopic("Bear", "🐻"),
              new DefaultTopic("Panda", "🐼"),
              new DefaultTopic("Koala", "🐨"),
              new DefaultTopic("Tiger", "🐅"),
              new DefaultTopic("Lion", "🦁"),
              new DefaultTopic("Horse", "🐎"),
              new DefaultTopic("Unicorn", "🦄"),
              new DefaultTopic("Cow", "🐄"),
              new DefaultTopic("Pig", "🐖"),
              new DefaultTopic("Sheep", "🐑"),
              new DefaultTopic("Goat", "🐐"),
              new DefaultTopic("Deer", "🦌"),
              new DefaultTopic("Rabbit", "🐇"),
              new DefaultTopic("Mouse", "🐭"),
              new DefaultTopic("Hamster", "🐹"),
              new DefaultTopic("Squirrel", "🐿️"),
              new DefaultTopic("Hedgehog", "🦔"),
              new DefaultTopic("Bat", "🦇"),
              new DefaultTopic("Monkey", "🐒"),
              new DefaultTopic("Gorilla", "🦍"),
              new DefaultTopic("Orangutan", "🦧"),
              new DefaultTopic("Elephant", "🐘"),
              new DefaultTopic("Rhino", "🦏"),
              new DefaultTopic("Hippo", "🦛"),
              new DefaultTopic("Giraffe", "🦒"),
              new DefaultTopic("Zebra", "🦓"),
              new DefaultTopic("Kangaroo", "🦘"),
              new DefaultTopic("Otter", "🦦"),
              new DefaultTopic("Sloth", "🦥"),
              new DefaultTopic("Raccoon", "🦝"),
              new DefaultTopic("Skunk", "🦨"),
              new DefaultTopic("Beaver", "🦫"),
              new DefaultTopic("Camel", "🐫"),
              new DefaultTopic("Dolphin", "🐬"),
              new DefaultTopic("Shark", "🦈"),
              new DefaultTopic("Octopus", "🐙"),
              new DefaultTopic("Seal", "🦭"),
              new DefaultTopic("Turtle", "🐢"),
              new DefaultTopic("Snake", "🐍"),
              new DefaultTopic("Crocodile", "🐊"),
              new DefaultTopic("Lizard", "🦎"),
              new DefaultTopic("Frog", "🐸"),
              new DefaultTopic("Spider", "🕷️"),
              new DefaultTopic("Ant", "🐜"),

              // --- ENTERTAINMENT ---
              new DefaultTopic("Entertainment", "🎬"),
              new DefaultTopic("Movies", "🎥"),
              new DefaultTopic("TV Shows", "📺"),
              new DefaultTopic("Streaming", "🍿"),
              new DefaultTopic("Netflix", "🍿"),
              new DefaultTopic("Anime", "🎌"),
              new DefaultTopic("Manga", "📚"),
              new DefaultTopic("Comics", "🗯️"),
              new DefaultTopic("Marvel", "🦸"),
              new DefaultTopic("Star Wars", "⭐"),

              // --- MUSIC ---
              new DefaultTopic("Music", "🎵"),
              new DefaultTopic("Rock", "🎸"),
              new DefaultTopic("Hip Hop", "🎤"),
              new DefaultTopic("Rap", "🎙️"),
              new DefaultTopic("Techno", "🎧"),
              new DefaultTopic("Electronic Music", "🎹"),
              new DefaultTopic("Jazz", "🎷"),
              new DefaultTopic("Classical Music", "🎻"),
              new DefaultTopic("K-Pop", "🇰🇷"),
              new DefaultTopic("Concerts", "🎫"),

              // --- GAMING ---
              new DefaultTopic("Gaming", "🎮"),
              new DefaultTopic("Esports", "🏆"),
              new DefaultTopic("Nintendo", "🎮"),
              new DefaultTopic("PlayStation", "🎮"),
              new DefaultTopic("Xbox", "🎮"),
              new DefaultTopic("PC Gaming", "🖥️"),
              new DefaultTopic("Retro Gaming", "🕹️"),
              new DefaultTopic("Indie Games", "🎲"),
              new DefaultTopic("RPG", "⚔️"),
              new DefaultTopic("FPS", "🎯"),

              // --- SPORTS ---
              new DefaultTopic("Sports", "🏅"),
              new DefaultTopic("Football", "⚽"),
              new DefaultTopic("Soccer", "⚽"),
              new DefaultTopic("American Football", "⚽"),
              new DefaultTopic("Basketball", "🏀"),
              new DefaultTopic("Tennis", "🎾"),
              new DefaultTopic("Formula 1", "🏎️"),
              new DefaultTopic("Cycling", "🚴"),
              new DefaultTopic("Running", "🏃"),
              new DefaultTopic("Fitness", "🏋️"),
              new DefaultTopic("Bodybuilding", "💪"),
              new DefaultTopic("Martial Arts", "🥋"),

              // --- LIFESTYLE ---
              new DefaultTopic("Lifestyle", "✨"),
              new DefaultTopic("Travel", "✈️"),
              new DefaultTopic("Food", "🍜"),
              new DefaultTopic("Cooking", "🍳"),
              new DefaultTopic("Coffee", "☕"),
              new DefaultTopic("Photography", "📸"),
              new DefaultTopic("Fashion", "👗"),
              new DefaultTopic("Design", "🎨"),
              new DefaultTopic("Architecture", "🏛️"),
              new DefaultTopic("Minimalism", "⬜"),

              // --- HEALTH ---
              new DefaultTopic("Health", "🩺"),
              new DefaultTopic("Mental Health", "💚"),
              new DefaultTopic("Wellness", "🧘"),
              new DefaultTopic("Nutrition", "🥗"),
              new DefaultTopic("Meditation", "🧘"),
              new DefaultTopic("Yoga", "🧘"),
              new DefaultTopic("Sleep", "😴"),
              new DefaultTopic("Self Improvement", "📈"),

              // --- STYLE / MOOD ---
              new DefaultTopic("Educational", "📘"),
              new DefaultTopic("Funny", "😂"),
              new DefaultTopic("Serious", "🧐"),
              new DefaultTopic("Inspirational", "✨"),
              new DefaultTopic("Motivational", "💪"),
              new DefaultTopic("Wholesome", "❤️"),
              new DefaultTopic("Controversial", "🔥"),
              new DefaultTopic("Satire", "🎭"),

              // --- MEDIA FORMAT ---
              new DefaultTopic("Video", "🎥"),
              new DefaultTopic("Photo", "📸"),
              new DefaultTopic("Article", "📄"),
              new DefaultTopic("Thread", "🧵"),
              new DefaultTopic("Blog", "✍️"),
              new DefaultTopic("Shortform", "📱"),

              // --- REGIONS ---
              new DefaultTopic("Europe", "🇪🇺"),
              new DefaultTopic("USA", "🇺🇸"),
              new DefaultTopic("Germany", "🇩🇪"),
              new DefaultTopic("Austria", "🇦🇹"),
              new DefaultTopic("France", "🇫🇷"),
              new DefaultTopic("UK", "🇬🇧"),
              new DefaultTopic("China", "🇨🇳"),
              new DefaultTopic("Japan", "🇯🇵"),
              new DefaultTopic("Korea", "🇰🇷"),
              new DefaultTopic("India", "🇮🇳"),

              // --- LANGUAGES ---
              new DefaultTopic("English", "🇬🇧"),
              new DefaultTopic("German", "🇩🇪"),
              new DefaultTopic("Chinese", "🇨🇳"),
              new DefaultTopic("Japanese", "🇯🇵"),
              new DefaultTopic("Korean", "🇰🇷"),
              new DefaultTopic("French", "🇫🇷"),
              new DefaultTopic("Spanish", "🇪🇸"),
              new DefaultTopic("Portuguese", "🇵🇹"),
              new DefaultTopic("Russian", "🇷🇺"),
              new DefaultTopic("Arabic", "🇸🇦")
              );

      for (DefaultTopic dt : defaultTopics) {
        if (!topicRepository.existsByName(dt.name())) {
          try {
            log.info("Seeding topic: {}", dt.name());
            // WICHTIG: Hier wird der externe Service aufgerufen
            float[] vector = embeddingService.embedText(dt.name());

            Topic topic = Topic.builder()
                    .name(dt.name())
                    .emoji(dt.emoji())
                    .embedding(vector)
                    .build();

            topicRepository.save(topic);
          } catch (Exception e) {
            log.error("Could not seed topic {}: {}", dt.name(), e.getMessage());
          }
        }
      }
      log.info("Topic seeding finished.");
    };
  }

  private record DefaultTopic(String name, String emoji) {}
}