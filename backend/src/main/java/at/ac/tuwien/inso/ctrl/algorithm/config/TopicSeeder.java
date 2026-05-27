package at.ac.tuwien.inso.ctrl.algorithm.config;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import java.util.List;
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
      var defaultTopics =
          List.of(
              // --- SCIENCE (9) ---
              new DefaultTopic(
                  "Science",
                  "🔬",
                  "Scientific discoveries, research findings, experiments, and general science news across all disciplines"),
              new DefaultTopic(
                  "Physics",
                  "⚛️",
                  "Physics, mechanics, thermodynamics, electromagnetism, forces, energy, and physical laws of the universe"),
              new DefaultTopic(
                  "Biology",
                  "🧬",
                  "Biology, life sciences, cell biology, genetics, evolution, organisms, and living systems"),
              new DefaultTopic(
                  "Chemistry",
                  "⚗️",
                  "Chemistry, chemical reactions, molecules, compounds, laboratory experiments, and materials science"),
              new DefaultTopic(
                  "Mathematics",
                  "📐",
                  "Mathematics, algebra, calculus, geometry, statistics, proofs, and mathematical theory"),
              new DefaultTopic(
                  "Neuroscience",
                  "🧠",
                  "Neuroscience, brain research, cognitive science, neural mechanisms, and how the nervous system works"),
              new DefaultTopic(
                  "Psychology",
                  "💭",
                  "Psychology, human behavior, mental processes, cognitive biases, personality, and social psychology"),
              new DefaultTopic(
                  "Astronomy",
                  "🔭",
                  "Astronomy, telescopes, stars, galaxies, planets, the universe, and observational sky science"),
              new DefaultTopic(
                  "Ecology",
                  "🌍",
                  "Ecology, ecosystems, biodiversity, food chains, environmental science, and species interactions"),

              // --- SPACE (4) ---
              new DefaultTopic(
                  "Space",
                  "🚀",
                  "Space exploration, NASA, ESA, missions, the cosmos, satellites, and everything beyond Earth's atmosphere"),
              new DefaultTopic(
                  "Rocketry",
                  "🚀",
                  "Rockets, launch vehicles, SpaceX, rocket engineering, propulsion, and commercial spaceflight"),
              new DefaultTopic(
                  "Mars",
                  "🔴",
                  "Mars exploration, Mars missions, colonization plans, the red planet, and life on Mars research"),
              new DefaultTopic(
                  "Black Holes",
                  "🕳️",
                  "Black holes, event horizons, gravitational singularities, dark matter, and extreme space phenomena"),

              // --- TECHNOLOGY (8) ---
              new DefaultTopic(
                  "Technology",
                  "💻",
                  "Technology news, gadgets, tech industry, product launches, digital transformation, and innovation"),
              new DefaultTopic(
                  "Programming",
                  "💻",
                  "Programming, coding, software development, algorithms, debugging, and developer tools"),
              new DefaultTopic(
                  "Web Development",
                  "🌐",
                  "Web development, frontend, backend, HTML, CSS, JavaScript, frameworks, and building websites"),
              new DefaultTopic(
                  "Cybersecurity",
                  "🔐",
                  "Cybersecurity, hacking, data breaches, vulnerabilities, digital privacy protection, and infosec"),
              new DefaultTopic(
                  "Privacy",
                  "🔒",
                  "Digital privacy, data protection, surveillance, personal data rights, and online security"),
              new DefaultTopic(
                  "Open Source",
                  "📂",
                  "Open source software, Linux, community-driven development, free software, and GitHub projects"),
              new DefaultTopic(
                  "Cloud Computing",
                  "☁️",
                  "Cloud computing, AWS, Azure, Google Cloud, serverless architecture, and infrastructure"),
              new DefaultTopic(
                  "Hardware",
                  "💾",
                  "Hardware, CPUs, GPUs, computer components, consumer electronics, and physical tech products"),

              // --- ARTIFICIAL INTELLIGENCE (6) ---
              new DefaultTopic(
                  "Artificial Intelligence",
                  "🤖",
                  "Artificial intelligence, AI research, intelligent systems, AI applications, and the future of AI"),
              new DefaultTopic(
                  "Machine Learning",
                  "🧠",
                  "Machine learning, deep learning, neural networks, model training, and data-driven AI systems"),
              new DefaultTopic(
                  "LLM",
                  "📚",
                  "Large language models, GPT, Claude, ChatGPT, generative AI, text generation, and AI chatbots"),
              new DefaultTopic(
                  "Computer Vision",
                  "👁️",
                  "Computer vision, image recognition, object detection, visual AI, and image processing systems"),
              new DefaultTopic(
                  "Robotics",
                  "🤖",
                  "Robotics, autonomous robots, humanoid robots, robot engineering, and robotic automation"),
              new DefaultTopic(
                  "AI Ethics",
                  "⚖️",
                  "AI ethics, bias in AI, responsible AI, AI regulation, alignment, and societal impact of artificial intelligence"),

              // --- BUSINESS & FINANCE (9) ---
              new DefaultTopic(
                  "Business",
                  "💼",
                  "Business news, corporate strategy, management, company culture, and the business world"),
              new DefaultTopic(
                  "Economics",
                  "📈",
                  "Economics, macroeconomics, microeconomics, markets, economic policy, and financial systems"),
              new DefaultTopic(
                  "Finance",
                  "💰",
                  "Personal finance, financial planning, budgeting, loans, banking, and money management"),
              new DefaultTopic(
                  "Investing",
                  "📊",
                  "Investing, stock market, portfolios, trading strategies, ETFs, dividends, and wealth building"),
              new DefaultTopic(
                  "Startups",
                  "🚀",
                  "Startups, venture capital, founders, product launches, entrepreneurship, and the startup ecosystem"),
              new DefaultTopic(
                  "Entrepreneurship",
                  "💡",
                  "Entrepreneurship, building a business, side hustles, solopreneurs, and starting a company from scratch"),
              new DefaultTopic(
                  "Marketing",
                  "📣",
                  "Marketing, branding, advertising, growth hacking, content marketing, and reaching audiences"),
              new DefaultTopic(
                  "Cryptocurrency",
                  "₿",
                  "Cryptocurrency, Bitcoin, Ethereum, blockchain, DeFi, NFTs, crypto markets, and digital assets"),
              new DefaultTopic(
                  "Real Estate",
                  "🏠",
                  "Real estate, property investment, housing market, renting, buying homes, and real estate trends"),

              // --- POLITICS & SOCIETY (8) ---
              new DefaultTopic(
                  "Politics",
                  "🏛️",
                  "Politics, political news, elections, parties, politicians, policy, and democratic processes"),
              new DefaultTopic(
                  "Geopolitics",
                  "🌍",
                  "Geopolitics, international relations, global conflicts, diplomacy, and world power dynamics"),
              new DefaultTopic(
                  "Law",
                  "⚖️",
                  "Law, legal cases, court rulings, legislation, legal rights, and the justice system"),
              new DefaultTopic(
                  "Human Rights",
                  "✊",
                  "Human rights, civil liberties, social justice, activism, discrimination, and equality movements"),
              new DefaultTopic(
                  "Education",
                  "🎓",
                  "Education, learning, schools, universities, pedagogy, curricula, and the future of education"),
              new DefaultTopic(
                  "Society",
                  "👥",
                  "Society, social trends, demographics, community, social issues, and how people live together"),
              new DefaultTopic(
                  "History",
                  "📜",
                  "History, historical events, ancient civilizations, wars, historical figures, and lessons from the past"),
              new DefaultTopic(
                  "Culture",
                  "🏺",
                  "Culture, cultural trends, traditions, art, literature, cultural identity, and global cultural exchange"),

              // --- ENVIRONMENT (5) ---
              new DefaultTopic(
                  "Climate",
                  "🌍",
                  "Climate change, global warming, climate policy, greenhouse gases, and environmental impact"),
              new DefaultTopic(
                  "Sustainability",
                  "♻️",
                  "Sustainability, zero waste, recycling, eco-friendly living, green products, and sustainable practices"),
              new DefaultTopic(
                  "Renewable Energy",
                  "☀️",
                  "Renewable energy, solar power, wind energy, clean energy transition, and sustainable power sources"),
              new DefaultTopic(
                  "Electric Vehicles",
                  "🚗",
                  "Electric vehicles, EVs, Tesla, EV charging infrastructure, battery technology, and electric mobility"),
              new DefaultTopic(
                  "Conservation",
                  "🌲",
                  "Wildlife conservation, nature preservation, endangered species, national parks, and protecting ecosystems"),

              // --- ANIMALS (7) ---
              new DefaultTopic(
                  "Cat",
                  "🐈",
                  "Cats, kittens, felines, pet cats, funny cat videos, cat behavior, and cute cat content"),
              new DefaultTopic(
                  "Dog",
                  "🐕",
                  "Dogs, puppies, dog breeds, pet dogs, dog training, cute dog videos, and dog behavior"),
              new DefaultTopic(
                  "Pets",
                  "🐾",
                  "Pets, pet care, pet owners, small animals, hamsters, rabbits, guinea pigs, and companion animals"),
              new DefaultTopic(
                  "Wild Animals",
                  "🦁",
                  "Wild animals, wildlife, safari, predators, mammals, jungle animals, and animals in nature"),
              new DefaultTopic(
                  "Birds",
                  "🐦",
                  "Birds, birdwatching, parrots, eagles, owls, penguins, ducks, and avian life"),
              new DefaultTopic(
                  "Marine Life",
                  "🐬",
                  "Marine life, ocean animals, dolphins, whales, sharks, fish, coral reefs, and sea creatures"),
              new DefaultTopic(
                  "Reptiles",
                  "🐍",
                  "Reptiles, snakes, lizards, turtles, crocodiles, frogs, and cold-blooded animals"),

              // --- ENTERTAINMENT (5) ---
              new DefaultTopic(
                  "Movies",
                  "🎥",
                  "Movies, films, cinema, movie reviews, box office, directors, actors, and film culture"),
              new DefaultTopic(
                  "TV Shows",
                  "📺",
                  "TV shows, series, episodes, binge-watching, television, show reviews, and streaming series"),
              new DefaultTopic(
                  "Anime",
                  "🎌",
                  "Anime, manga, Japanese animation, anime series, anime characters, and otaku culture"),
              new DefaultTopic(
                  "Comics",
                  "🗯️",
                  "Comics, graphic novels, Marvel, DC, superhero stories, comic art, and sequential storytelling"),
              new DefaultTopic(
                  "Streaming",
                  "🍿",
                  "Streaming services, Netflix, YouTube, Twitch, Disney+, online video, and digital media consumption"),

              // --- MUSIC (7) ---
              new DefaultTopic(
                  "Music",
                  "🎵",
                  "Music, songs, artists, albums, music news, concerts, and the music industry"),
              new DefaultTopic(
                  "Rock",
                  "🎸",
                  "Rock music, rock bands, guitar, classic rock, metal, punk, and rock culture"),
              new DefaultTopic(
                  "Hip Hop",
                  "🎤",
                  "Hip hop, rap, rappers, beats, trap, R&B, rap lyrics, and hip hop culture"),
              new DefaultTopic(
                  "Electronic Music",
                  "🎹",
                  "Electronic music, techno, EDM, house music, DJs, synthesizers, and rave culture"),
              new DefaultTopic(
                  "Jazz",
                  "🎷",
                  "Jazz, jazz musicians, improvisation, blues, swing, bebop, and jazz culture"),
              new DefaultTopic(
                  "Classical Music",
                  "🎻",
                  "Classical music, orchestras, composers, opera, symphonies, and classical performances"),
              new DefaultTopic(
                  "K-Pop",
                  "🇰🇷",
                  "K-Pop, Korean pop music, K-Pop groups, K-Pop idols, BTS, BLACKPINK, and Korean entertainment"),

              // --- GAMING (6) ---
              new DefaultTopic(
                  "Gaming",
                  "🎮",
                  "Video games, gaming news, game releases, gaming culture, consoles, and the gaming community"),
              new DefaultTopic(
                  "Esports",
                  "🏆",
                  "Esports, competitive gaming, tournaments, professional players, and esports events"),
              new DefaultTopic(
                  "PC Gaming",
                  "🖥️",
                  "PC gaming, gaming rigs, computer specs, PC builds, Steam, and gaming peripherals"),
              new DefaultTopic(
                  "Retro Gaming",
                  "🕹️",
                  "Retro gaming, classic games, old-school consoles, nostalgia, NES, SNES, and gaming history"),
              new DefaultTopic(
                  "RPG",
                  "⚔️",
                  "RPG games, role-playing games, open world, fantasy RPGs, JRPGs, and story-driven games"),
              new DefaultTopic(
                  "Indie Games",
                  "🎲",
                  "Indie games, independent game developers, small studios, creative games, and indie game releases"),

              // --- SPORTS (8) ---
              new DefaultTopic(
                  "Sports",
                  "🏅",
                  "Sports news, athletic competitions, sports events, athletes, and general sports culture"),
              new DefaultTopic(
                  "Football",
                  "⚽",
                  "Football, soccer, FIFA, football clubs, Premier League, Champions League, and football matches"),
              new DefaultTopic(
                  "American Football",
                  "🏈",
                  "American football, NFL, Super Bowl, touchdowns, quarterbacks, and gridiron football"),
              new DefaultTopic(
                  "Basketball",
                  "🏀",
                  "Basketball, NBA, dunks, NBA players, basketball teams, playoffs, and basketball culture"),
              new DefaultTopic(
                  "Tennis",
                  "🎾",
                  "Tennis, Grand Slams, Wimbledon, tennis players, ATP, WTA, and tennis matches"),
              new DefaultTopic(
                  "Formula 1",
                  "🏎️",
                  "Formula 1, F1 racing, race drivers, Grand Prix, F1 teams, pit stops, and motorsport"),
              new DefaultTopic(
                  "Fitness",
                  "🏋️",
                  "Fitness, workouts, gym, exercise routines, bodybuilding, weight loss, running, and healthy living"),
              new DefaultTopic(
                  "Martial Arts",
                  "🥋",
                  "Martial arts, MMA, UFC, boxing, kickboxing, wrestling, judo, and combat sports"),

              // --- LIFESTYLE (6) ---
              new DefaultTopic(
                  "Travel",
                  "✈️",
                  "Travel, destinations, travel tips, backpacking, hotels, tourism, road trips, and travel photography"),
              new DefaultTopic(
                  "Food",
                  "🍜",
                  "Food, cuisine, restaurants, food culture, street food, food trends, and eating experiences"),
              new DefaultTopic(
                  "Cooking",
                  "🍳",
                  "Cooking, recipes, kitchen tips, meal prep, baking, chef techniques, and home cooking"),
              new DefaultTopic(
                  "Coffee",
                  "☕",
                  "Coffee, espresso, coffee culture, cafes, latte art, brewing methods, and specialty coffee"),
              new DefaultTopic(
                  "Fashion",
                  "👗",
                  "Fashion, style, clothing, trends, designers, streetwear, outfits, and the fashion industry"),
              new DefaultTopic(
                  "Design",
                  "🎨",
                  "Design, graphic design, UI/UX, art direction, typography, visual design, and creative work"),

              // --- HEALTH (6) ---
              new DefaultTopic(
                  "Health",
                  "🩺",
                  "Health, medical news, wellness, disease prevention, healthcare, and physical well-being"),
              new DefaultTopic(
                  "Mental Health",
                  "💚",
                  "Mental health, anxiety, depression, therapy, mindfulness, meditation, yoga, and emotional well-being"),
              new DefaultTopic(
                  "Nutrition",
                  "🥗",
                  "Nutrition, healthy eating, diet, vitamins, superfoods, meal planning, and food science"),
              new DefaultTopic(
                  "Sleep",
                  "😴",
                  "Sleep, sleep quality, insomnia, sleep science, napping, circadian rhythms, and rest"),
              new DefaultTopic(
                  "Self Improvement",
                  "📈",
                  "Self improvement, personal development, habits, productivity, goal setting, and becoming a better version of yourself"),
              new DefaultTopic(
                  "Wellness",
                  "🧘",
                  "Wellness, holistic health, self-care, relaxation, balance, and living a healthy, intentional life"),

              // --- VISUAL ARTS (4) ---new DefaultTopic("Art",          "🎨", "Art, fine arts,
              // painting, drawing, sculpture, watercolor, oil painting, galleries, museums, and
              // artistic expression"),
              new DefaultTopic(
                  "Digital Art",
                  "🖥️",
                  "Digital art, digital illustration, concept art, 3D art, digital painting, AI-generated art, and computer-generated visuals"),
              new DefaultTopic(
                  "Photography",
                  "📸",
                  "Photography, photos, camera gear, photo editing, composition, portrait photography, and visual storytelling"),
              new DefaultTopic(
                  "Street Art",
                  "🏙️",
                  "Street art, graffiti, murals, urban art, Banksy, spray paint, and public art installations"),

              // --- BOOKS & LITERATURE (4) ---
              new DefaultTopic(
                  "Books",
                  "📚",
                  "Books, reading, book reviews, literature, novels, non-fiction, and book recommendations"),
              new DefaultTopic(
                  "Fiction",
                  "📖",
                  "Fiction, novels, storytelling, short stories, literary fiction, thrillers, horror, and fantasy books"),
              new DefaultTopic(
                  "Poetry",
                  "🖊️",
                  "Poetry, poems, poets, spoken word, verse, lyrical writing, and poetic expression"),
              new DefaultTopic(
                  "Writing",
                  "✍️",
                  "Writing, creative writing, copywriting, screenwriting, storytelling craft, and the writing process"),

              // --- PHILOSOPHY (1) ---
              new DefaultTopic(
                  "Philosophy",
                  "🤔",
                  "Philosophy, philosophical ideas, ethics, logic, existentialism, stoicism, and great thinkers"),

              // --- SCI-FI & FANTASY (3) ---
              new DefaultTopic(
                  "Science Fiction",
                  "🚀",
                  "Science fiction, sci-fi books, sci-fi films and series, dystopia, cyberpunk, space opera, and futurism"),
              new DefaultTopic(
                  "Fantasy",
                  "🧙",
                  "Fantasy, high fantasy, magic systems, dragons, epic fantasy books and series, worldbuilding, and folklore"),
              new DefaultTopic(
                  "Horror",
                  "👻",
                  "Horror, scary content, horror movies, creepypasta, ghost stories, psychological horror, and the macabre"),

              // --- AUTOMOTIVE (3) ---
              new DefaultTopic(
                  "Cars",
                  "🚗",
                  "Cars, car reviews, car culture, automotive news, supercars, classic cars, and car enthusiasts"),
              new DefaultTopic(
                  "Motorcycles",
                  "🏍️",
                  "Motorcycles, motorbikes, moto culture, bike reviews, motorcycle touring, and two-wheel riding"),
              new DefaultTopic(
                  "Aviation",
                  "✈️",
                  "Aviation, aircraft, piloting, planespotting, airports, airline industry, and flying culture"),

              // --- OUTDOORS & ADVENTURE (4) ---
              new DefaultTopic(
                  "Hiking",
                  "🥾",
                  "Hiking, trekking, trails, mountain hiking, backpacking, hiking gear, and outdoor walking adventures"),
              new DefaultTopic(
                  "Camping",
                  "⛺",
                  "Camping, campfires, outdoor survival, bushcraft, van life, overlanding, and sleeping in nature"),
              new DefaultTopic(
                  "Climbing",
                  "🧗",
                  "Rock climbing, bouldering, mountaineering, climbing gear, indoor climbing, and summit expeditions"),
              new DefaultTopic(
                  "Nature",
                  "🌿",
                  "Nature, landscapes, forests, mountains, rivers, wildlife in nature, and the beauty of the natural world"),

              // --- ARCHITECTURE & INTERIOR (2) ---
              new DefaultTopic(
                  "Architecture",
                  "🏛️",
                  "Architecture, buildings, architectural design, famous structures, urban planning, and architectural history"),
              new DefaultTopic(
                  "Interior Design",
                  "🛋️",
                  "Interior design, home decor, room aesthetics, furniture, minimalism, cozy spaces, and decorating ideas"),

              // --- DIY & CRAFTS (4) ---
              new DefaultTopic(
                  "DIY",
                  "🔨",
                  "DIY, do it yourself, home improvement, repairs, building projects, upcycling, and hands-on making"),
              new DefaultTopic(
                  "Crafts",
                  "🧵",
                  "Crafts, handmade, knitting, crocheting, sewing, paper crafts, and creative handcraft projects"),
              new DefaultTopic(
                  "Woodworking",
                  "🪵",
                  "Woodworking, carpentry, furniture making, wood carving, joinery, and crafting with wood"),
              new DefaultTopic(
                  "3D Printing",
                  "🖨️",
                  "3D printing, additive manufacturing, 3D models, FDM, resin printing, and maker culture"),

              // --- TRUE CRIME (1) ---
              new DefaultTopic(
                  "True Crime",
                  "🔍",
                  "True crime, criminal cases, murder mysteries, investigations, documentaries, and real criminal stories"),

              // --- PARENTING & FAMILY (2) ---
              new DefaultTopic(
                  "Parenting",
                  "👶",
                  "Parenting, raising children, baby care, toddlers, parenting tips, family life, and child development"),
              new DefaultTopic(
                  "Family",
                  "👨‍👩‍👧",
                  "Family, relationships, home life, siblings, grandparents, family activities, and family dynamics"),

              // --- SPIRITUALITY & RELIGION (2) ---
              new DefaultTopic(
                  "Spirituality",
                  "🕊️",
                  "Spirituality, mindfulness, spiritual practices, inner peace, consciousness, and personal spiritual journeys"),
              new DefaultTopic(
                  "Religion",
                  "🙏",
                  "Religion, faith, religious traditions, theology, Christianity, Islam, Judaism, Buddhism, Hinduism, and more"),

              // --- THEATER & DANCE (2) ---
              new DefaultTopic(
                  "Theater",
                  "🎭",
                  "Theater, stage plays, musicals, acting, Broadway, opera, live performance, and theatrical arts"),
              new DefaultTopic(
                  "Dance",
                  "💃",
                  "Dance, dancing, choreography, ballet, hip hop dance, contemporary dance, and dance performances"),

              // --- LGBTQ+ (1) ---
              new DefaultTopic(
                  "LGBTQ+",
                  "🏳️‍🌈",
                  "LGBTQ+, queer culture, pride, gay rights, transgender topics, queer art, and LGBTQ+ community content"),

              // --- STYLE / MOOD (4) ---
              // Kept only those that users would genuinely follow as interest filters.
              // Removed: Serious, Wholesome, Motivational (too generic).
              // Note: Meme, Tutorial, Guide etc. should live in PostLabel (content-type), not here.
              new DefaultTopic(
                  "Funny",
                  "😂",
                  "Funny content, humor, comedy, jokes, memes, satire, and content that makes you laugh"),
              new DefaultTopic(
                  "Inspirational",
                  "✨",
                  "Inspirational content, motivational stories, uplifting posts, and content that gives you energy"),
              new DefaultTopic(
                  "Controversial",
                  "🔥",
                  "Controversial topics, hot takes, debate-worthy content, and posts that spark strong reactions"),
              new DefaultTopic(
                  "Satire",
                  "🎭",
                  "Satire, parody, irony, comedic commentary on current events, and satirical news"));

      List<DefaultTopic> topicsToSeed =
          defaultTopics.stream().filter(dt -> !topicRepository.existsByName(dt.name())).toList();

      if (topicsToSeed.isEmpty()) {
        log.info("Topic seeding finished. No new topics to seed.");
        return;
      }

      log.info("Batch seeding {} new topics...", topicsToSeed.size());

      try {
        // Use the richer description for embedding instead of just the name.
        // This closes the semantic gap between single-word topic names and full post captions.
        List<String> topicTexts =
            topicsToSeed.stream().map(dt -> dt.name() + ": " + dt.description()).toList();

        List<float[]> embeddings = embeddingService.embedText(topicTexts);

        List<Topic> topicsToSave = new java.util.ArrayList<>();
        for (int i = 0; i < topicsToSeed.size(); i++) {
          DefaultTopic dt = topicsToSeed.get(i);
          float[] vector = embeddings.get(i);
          topicsToSave.add(
              Topic.builder().name(dt.name()).emoji(dt.emoji()).embedding(vector).build());
        }

        topicRepository.saveAll(topicsToSave);
        log.info("Topic seeding successfully finished.");
      } catch (Exception e) {
        log.error("Could not seed topics in batch: {}", e.getMessage(), e);
      }
    };
  }

  private record DefaultTopic(String name, String emoji, String description) {}
}
