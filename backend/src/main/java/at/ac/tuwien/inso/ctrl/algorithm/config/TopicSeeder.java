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
                              "🦋",
                              // FIX: unique emoji (was 🌍, shared with Geopolitics/Climate/Africa)
                              "Ecology, ecosystems, biodiversity, food chains, environmental science, and species interactions"),

                      // --- SPACE (4) ---
                      new DefaultTopic(
                              "Space",
                              "🚀",
                              "Space exploration, NASA, ESA, missions, the cosmos, satellites, and everything beyond Earth's atmosphere"),
                      new DefaultTopic(
                              "Rocketry",
                              "🛸",
                              // FIX: unique emoji (was 🚀, shared with Space/Startups/Science Fiction)
                              "Rockets, launch vehicles, SpaceX, rocket engineering, propulsion, and commercial spaceflight"),
                      new DefaultTopic(
                              "Mars",
                              "🔴",
                              "Mars exploration, Mars missions, colonization plans, the red planet, and life on Mars research"),
                      new DefaultTopic(
                              "Black Holes",
                              "🕳️",
                              // FIX: removed "dark matter" — unrelated concept, would mislead the embedding
                              "Black holes, event horizons, gravitational singularities, neutron stars, and extreme space phenomena"),

                      // --- TECHNOLOGY (8) ---
                      new DefaultTopic(
                              "Technology",
                              "💻",
                              "Technology news, gadgets, tech industry, product launches, digital transformation, and innovation"),
                      new DefaultTopic(
                              "Programming",
                              "👨‍💻",
                              // FIX: unique emoji (was 💻, shared with Technology)
                              "Programming, coding, software development, algorithms, debugging, and developer tools"),
                      new DefaultTopic(
                              "Web Development",
                              "🌐",
                              "Web development, frontend, backend, HTML, CSS, JavaScript, frameworks, and building websites"),
                      new DefaultTopic(
                              "Cybersecurity",
                              "🔐",
                              // FIX: description now scoped to attacks/breaches, not generic privacy — reduces overlap with Privacy topic
                              "Cybersecurity, hacking, data breaches, malware, vulnerabilities, penetration testing, and information security"),
                      new DefaultTopic(
                              "Privacy",
                              "🔒",
                              // FIX: removed "online security" to reduce overlap with Cybersecurity; now scoped to rights/surveillance
                              "Digital privacy, data protection, surveillance, personal data rights, GDPR, and the right to be forgotten"),
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
                              "🔧",
                              // FIX: emoji changed from 💾 (floppy disk) to 🔧 — more representative of modern hardware
                              "Hardware, CPUs, GPUs, computer components, consumer electronics, and physical tech products"),

                      // --- ARTIFICIAL INTELLIGENCE (6) ---
                      new DefaultTopic(
                              "Artificial Intelligence",
                              "🤖",
                              "Artificial intelligence, AI research, intelligent systems, AI applications, and the future of AI"),
                      new DefaultTopic(
                              "Machine Learning",
                              "⚙️",
                              // FIX: unique emoji (was 🧠, shared with Neuroscience)
                              "Machine learning, deep learning, neural networks, model training, and data-driven AI systems"),
                      new DefaultTopic(
                              "LLM",
                              "💬",
                              // FIX: unique emoji (was 📚, shared with Books)
                              "Large language models, GPT, Claude, ChatGPT, generative AI, text generation, and AI chatbots"),
                      new DefaultTopic(
                              "Computer Vision",
                              "👁️",
                              "Computer vision, image recognition, object detection, visual AI, and image processing systems"),
                      new DefaultTopic(
                              "Robotics",
                              "🦾",
                              // FIX: unique emoji (was 🤖, shared with Artificial Intelligence)
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
                              "📉",
                              // FIX: unique emoji (was 📈, shared with Investing/Self Improvement); description scoped to macro/micro theory
                              "Macroeconomics, microeconomics, economic policy, markets, inflation, GDP, and financial systems — not personal finance or investing"),
                      new DefaultTopic(
                              "Finance",
                              "💰",
                              // FIX: description scoped to personal money management, not markets or theory
                              "Personal finance, financial planning, budgeting, loans, banking, and money management — not macroeconomics or trading"),
                      new DefaultTopic(
                              "Investing",
                              "📊",
                              // FIX: description scoped to markets/portfolios, not personal budgeting or theory
                              "Investing, stock market, portfolios, trading strategies, ETFs, dividends, and wealth building — not personal budgeting or economic theory"),
                      new DefaultTopic(
                              "Startups",
                              "🌱",
                              // FIX: unique emoji (was 🚀, shared with Space/Rocketry/Science Fiction); description scoped to VC-funded tech startups
                              "Startups, venture capital, seed funding, product-market fit, founders, and the tech startup ecosystem — not general self-employment"),
                      new DefaultTopic(
                              "Entrepreneurship",
                              "💡",
                              // FIX: description now scoped to general business building and solopreneurs, not VC-funded startups
                              "Entrepreneurship, building a business, side hustles, solopreneurs, small business, and starting a company from scratch — not VC-funded tech startups"),
                      new DefaultTopic(
                              "Marketing",
                              "📣",
                              "Marketing, branding, advertising, growth hacking, content marketing, and reaching audiences"),
                      new DefaultTopic(
                              "Cryptocurrency",
                              "₿",
                              // FIX: removed NFTs (largely obsolete trend); kept blockchain/DeFi as still relevant
                              "Cryptocurrency, Bitcoin, Ethereum, blockchain, DeFi, crypto markets, and digital assets"),
                      new DefaultTopic(
                              "Real Estate",
                              "🏠",
                              "Real estate, property investment, housing market, renting, buying homes, and real estate trends"),
                      // --- NEWS (1) ---
                      new DefaultTopic(
                              "News",
                              "📰",
                              "Breaking news, current events, daily news updates, journalism, headlines, and news from around the world — Nachrichten, actualités, noticias, notícias"),
                      // --- POLITICS & SOCIETY (8) ---
                      new DefaultTopic(
                              "Politics",
                              "🏛️",
                              "Politics, political news, elections, parties, politicians, policy, and democratic processes"),
                      new DefaultTopic(
                              "Geopolitics",
                              "🌐",
                              // FIX: unique emoji (was 🌍, shared with Ecology/Climate/Africa)
                              "Geopolitics, international relations, global conflicts, diplomacy, and world power dynamics"),
                      new DefaultTopic(
                              "Law",
                              "📋",
                              // FIX: unique emoji (was ⚖️, shared with AI Ethics)
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
                              // FIX: description tightened to social trends/demographics; reduced overlap with Culture
                              "Society, social trends, demographics, community, social issues, inequality, and how people live together — not cultural traditions or art"),
                      new DefaultTopic(
                              "History",
                              "📜",
                              "History, historical events, ancient civilizations, wars, historical figures, and lessons from the past"),
                      new DefaultTopic(
                              "Culture",
                              "🏺",
                              // FIX: tightened to traditions/cultural identity; reduced overlap with Society
                              "Culture, cultural traditions, art, literature, cultural identity, and global cultural exchange — not social policy or demographics"),

                      // --- ENVIRONMENT (5) ---
                      new DefaultTopic(
                              "Climate",
                              "🌡️",
                              // FIX: unique emoji (was 🌍, shared with Ecology/Geopolitics/Africa)
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
                              "🔋",
                              // FIX: unique emoji (was 🚗, shared with Cars)
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
                              // FIX: removed "frogs" — amphibians, not reptiles
                              "Reptiles, snakes, lizards, turtles, crocodiles, geckos, iguanas, and cold-blooded animals"),

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
                              // FIX: removed "R&B" — distinct genre that would pull R&B posts incorrectly here
                              "Hip hop, rap, rappers, beats, trap, rap lyrics, and hip hop culture"),
                      new DefaultTopic(
                              "Electronic Music",
                              "🎹",
                              "Electronic music, techno, EDM, house music, DJs, synthesizers, and rave culture"),
                      new DefaultTopic(
                              "Jazz",
                              "🎷",
                              "Jazz, jazz musicians, improvisation, swing, bebop, and jazz culture"),
                      new DefaultTopic(
                              "Classical Music",
                              "🎻",
                              "Classical music, orchestras, composers, symphonies, and classical performances"),
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
                              // FIX: removed "wrestling" — pro wrestling is entertainment, not a martial art
                              "Martial arts, MMA, UFC, boxing, kickboxing, judo, karate, and combat sports"),

                      // --- LIFESTYLE (6) ---
                      new DefaultTopic(
                              "Travel",
                              "✈️",
                              "Travel, destinations, travel tips, backpacking, hotels, tourism, road trips, and travel photography"),
                      new DefaultTopic(
                              "Food",
                              "🍜",
                              // FIX: scoped to culture/restaurants/experience, not recipes or nutrition science
                              "Food, cuisine, restaurants, food culture, street food, food trends, and eating experiences — not recipes or diet advice"),
                      new DefaultTopic(
                              "Cooking",
                              "🍳",
                              // FIX: scoped to recipes and techniques, not nutrition science or restaurant culture
                              "Cooking, recipes, kitchen tips, meal prep, baking, chef techniques, and home cooking — not nutrition or restaurant reviews"),
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
                              "🖌️",
                              // FIX: emoji changed from 🎨 (freed up for restored Art topic)
                              "Design, graphic design, UI/UX, art direction, typography, visual design, and creative work"),

                      // --- HEALTH (6) ---
                      new DefaultTopic(
                              "Health",
                              "🩺",
                              // FIX: tightened to medical/physical health, reduced overlap with Mental Health and Wellness
                              "Health, medical news, disease prevention, healthcare, physical well-being, and medical research — not mental health or lifestyle wellness"),
                      new DefaultTopic(
                              "Mental Health",
                              "💚",
                              // FIX: removed "yoga" (physical practice, not mental health); tightened to psychological topics
                              "Mental health, anxiety, depression, therapy, mindfulness, meditation, emotional well-being, and psychological health"),
                      new DefaultTopic(
                              "Nutrition",
                              "🥗",
                              // FIX: tightened to diet science, reduced overlap with Cooking/Food
                              "Nutrition, healthy eating, diet, vitamins, superfoods, meal planning, and food science — not recipes or restaurant culture"),
                      new DefaultTopic(
                              "Sleep",
                              "😴",
                              "Sleep, sleep quality, insomnia, sleep science, napping, circadian rhythms, and rest"),
                      new DefaultTopic(
                              "Self Improvement",
                              "🎯",
                              // FIX: unique emoji (was 📈, shared with Economics/Investing)
                              "Self improvement, personal development, habits, productivity, goal setting, and becoming a better version of yourself"),
                      new DefaultTopic(
                              "Wellness",
                              "🧘",
                              // FIX: added yoga here (moved from Mental Health); tightened to lifestyle/self-care
                              "Wellness, holistic health, self-care, yoga, relaxation, balance, and living a healthy, intentional life"),

                      // --- VISUAL ARTS (4) ---
                      new DefaultTopic(
                              "Art",
                              "🎨",
                              // FIX: was accidentally commented out — restored
                              "Art, fine arts, painting, drawing, sculpture, watercolor, oil painting, galleries, museums, and artistic expression"),
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
                              // FIX: removed "horror" — Horror has its own topic; keeping it here caused embedding overlap
                              "Fiction, novels, storytelling, short stories, literary fiction, thrillers, and fantasy books"),
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
                              "🌌",
                              // FIX: unique emoji (was 🚀, shared with Space/Rocketry/Startups)
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
                              "🏗️",
                              // FIX: unique emoji (was 🏛️, shared with Politics)
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
                      // Note: Meme, Tutorial, Guide etc. should live in PostLabel (content-type), not here.
                      new DefaultTopic(
                              "Funny",
                              "😂",
                              // FIX: removed "memes" — contradicted the PostLabel comment above; memes are a format, not a topic
                              "Funny content, humor, comedy, jokes, satire, and content that makes you laugh"),
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
                              "🃏",
                              // FIX: unique emoji (was 🎭, shared with Theater)
                              "Satire, parody, irony, comedic commentary on current events, and satirical news"),

                      // --- REGIONS (26) ---
                      // Continents / macro-regions
                      new DefaultTopic("Europe",        "🇪🇺", "Europe, European Union, European politics, culture, travel, and news from European countries — Europa, Europäische Union, europäische Politik, Europe, l'Europe"),
                      new DefaultTopic("North America",  "🌎", "North America, Canada, Mexico, USA, Caribbean, and news and culture from the North American continent"),
                      new DefaultTopic("Latin America",  "🌎", "Latin America, South America, Central America, Brazil, Argentina, Colombia, and Hispanic culture — América Latina, Sudamérica, cultura latinoamericana, noticias de América Latina"),
                      new DefaultTopic("Middle East",    "🌙", "Middle East, Arab world, Israel, Iran, Gulf states, geopolitics, culture, and news from the region — Naher Osten, Moyen-Orient, Oriente Medio, Medio Oriente"),
                      new DefaultTopic("Africa",         "🌍", "Africa, African culture, African politics, Sub-Saharan Africa, North Africa, and news from the continent — Afrique, Afrika, Africa, cultura africana"),
                      new DefaultTopic("Asia",           "🌏", "Asia, Asian culture, pan-Asian news, Southeast Asia, Central Asia, and the broader Asian continent — Asien, Asie, Asia, cultura asiatica"),
                      new DefaultTopic("Southeast Asia", "🌴", "Southeast Asia, Thailand, Vietnam, Indonesia, Philippines, Singapore, Malaysia, and the ASEAN region — Südostasien, Asie du Sud-Est, Sudeste Asiático"),
                      new DefaultTopic("Oceania",        "🦘", "Oceania, Australia, New Zealand, Pacific Islands, and culture and news from the Pacific region"),

                      // Individual countries — Europe
                      new DefaultTopic("USA",         "🇺🇸", "United States, American culture, US politics, news from America, and American society"),
                      new DefaultTopic("UK",          "🇬🇧", "United Kingdom, British culture, UK politics, Premier League, London, and news from Britain"),
                      new DefaultTopic("Germany",     "🇩🇪", "Germany, German culture, German politics, Bundesliga, news from Germany — Deutschland, deutsche Politik, deutsche Nachrichten, Bundesregierung, Bundesrat, Bundestag, deutsche Gesellschaft, Nachrichten aus Deutschland"),
                      new DefaultTopic("Austria",     "🇦🇹", "Austria, Austrian culture, Vienna, Austrian politics, news from Austria — Österreich, Wien, österreichische Politik, Nachrichten aus Österreich, österreichische Gesellschaft, Bundesregierung Österreich"),
                      new DefaultTopic("France",      "🇫🇷", "France, French culture, Paris, French politics, cuisine, fashion, and news from France — France, politique française, actualités françaises, culture française, Paris, société française"),
                      new DefaultTopic("Italy",       "🇮🇹", "Italy, Italian culture, food, fashion, Serie A, Italian politics, Rome, and news from Italy — Italia, politica italiana, cultura italiana, notizie dall'Italia, Roma, società italiana"),
                      new DefaultTopic("Spain",       "🇪🇸", "Spain, Spanish culture, La Liga, Barcelona, Madrid, Spanish politics, and news from Spain — España, política española, cultura española, noticias de España, sociedad española"),
                      new DefaultTopic("Switzerland", "🇨🇭", "Switzerland, Swiss culture, finance, Swiss politics, Alps, Zurich, Geneva, and news from Switzerland — Schweiz, Schweizer Politik, Nachrichten aus der Schweiz, Suisse, politique suisse, Svizzera"),
                      new DefaultTopic("Scandinavia", "❄️",  "Scandinavia, Nordic countries, Sweden, Norway, Denmark, Finland, and culture and news from the Nordics — Skandinavien, Sverige, Norge, Danmark, Finland, nordisk kultur, Norden"),
                      // FIX: emoji changed from 🇸🇪 (Swedish flag only) to ❄️ — neutral and representative of all Nordic countries
                      new DefaultTopic("Ukraine",     "🇺🇦", "Ukraine, Ukrainian culture, conflict, politics, Kyiv, and news from Ukraine — Ukraine, Krieg in der Ukraine, ukrainische Politik, Kiew, Russland Ukraine"),
                      new DefaultTopic("Russia",      "🇷🇺", "Russia, Russian culture, Russian politics, Moscow, geopolitics, and news from Russia — Russland, russische Politik, Moskau, Putin, Nachrichten aus Russland, Russie, Rusia"),
                      new DefaultTopic("Turkey",      "🇹🇷", "Turkey, Turkish culture, Istanbul, Turkish politics, economy, and news from Turkey — Türkei, Türkiye, türkische Politik, Istanbul, Nachrichten aus der Türkei"),

                      // Individual countries — Americas
                      new DefaultTopic("Canada",    "🇨🇦", "Canada, Canadian culture, Canadian politics, hockey, Toronto, Vancouver, and news from Canada — Canada, politique canadienne, culture canadienne, actualités canadiennes"),
                      new DefaultTopic("Brazil",    "🇧🇷", "Brazil, Brazilian culture, football, Carnival, Amazon, Brazilian politics, and news from Brazil — Brasil, cultura brasileira, política brasileira, notícias do Brasil, futebol brasileiro, Carnaval"),
                      new DefaultTopic("Mexico",    "🇲🇽", "Mexico, Mexican culture, food, football, Mexican politics, Mexico City, and news from Mexico — México, cultura mexicana, política mexicana, noticias de México, Ciudad de México"),

                      // Individual countries — Asia & Oceania
                      new DefaultTopic("China",      "🇨🇳", "China, Chinese culture, Chinese politics, technology, economy, and news from China — China, Zhongguo, chinesische Politik, Nachrichten aus China, Chine, politique chinoise"),
                      new DefaultTopic("Japan",      "🇯🇵", "Japan, Japanese culture, Tokyo, anime, technology, Japanese cuisine, and news from Japan — Japan, Nihon, japanische Kultur, Nachrichten aus Japan, Japon, culture japonaise, Tokyo"),
                      new DefaultTopic("South Korea","🇰🇷", "South Korea, Korean culture, K-Pop, K-drama, technology, Korean politics, Seoul, and news from Korea — Südkorea, Korea, koreanische Kultur, Hanguk, Seoul, K-Pop, K-Drama"),
                      new DefaultTopic("India",      "🇮🇳", "India, Indian culture, Bollywood, Indian politics, tech industry, cricket, and news from India — Indien, indische Kultur, Bharat, Bollywood, indische Politik, Nachrichten aus Indien"),
                      new DefaultTopic("Australia",  "🇦🇺", "Australia, Australian culture, sports, wildlife, Australian politics, Sydney, and news from Australia — Australien, australische Kultur, Nachrichten aus Australien, Australie"));


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