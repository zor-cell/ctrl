package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.post.entity.*;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.LabelRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class MastodonScheduler {

    private static final String  SOURCE_MASTODON = "mastodon";
    private static final int     FETCH_LIMIT     = 20;
    private static final Pattern URL_PATTERN     = Pattern.compile("https?://\\S+");

    private final PostRepository           postRepository;
    private final ExternalAuthorRepository externalAuthorRepository;
    private final LabelRepository          labelRepository;
    private final TopicRepository          topicRepository;
    private final SourceRepository         sourceRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    public MastodonScheduler(PostRepository postRepository,
                             ExternalAuthorRepository externalAuthorRepository,
                             LabelRepository labelRepository,
                             TopicRepository topicRepository,
                             SourceRepository sourceRepository) {
        this.postRepository           = postRepository;
        this.externalAuthorRepository = externalAuthorRepository;
        this.labelRepository          = labelRepository;
        this.topicRepository          = topicRepository;
        this.sourceRepository         = sourceRepository;
    }

    @Scheduled(fixedRateString = "PT15M")
    @Transactional
    public void fetchMastodonPosts() {
        log.info("Mastodon scheduler started");
        fetchFromAccount("ard.social",      "tagesschau",           "Tagesschau", "📺");
        fetchFromAccount("mastodon.social", "BBCNews@flipboard.com","BBC News",   "📰");
        log.info("Mastodon scheduler finished");
    }

    private void fetchFromAccount(String instance, String acct, String sourceName, String sourceEmoji) {
        try {
            String accountId = lookupAccountId(instance, acct);
            if (accountId == null) {
                log.warn("Could not resolve account: {}@{}", acct, instance);
                return;
            }

            List<JsonNode> statuses = fetchStatuses(instance, accountId);

            // Labels die für alle Posts dieser Quelle gelten
            Label mastodonLabel = findOrCreateLabel("mastodon", "Mastodon", "🐘",
                    "Mastodon is a decentralized social media platform where this post was originally published.");
            Label sourceLabel = findOrCreateLabel(toId(sourceName), sourceName, sourceEmoji,
                    getExplanation(sourceName));
            int saved = 0;
            for (JsonNode status : statuses) {
                if (saveStatus(status, mastodonLabel, sourceLabel, sourceName)) saved++;
            }
            log.info("Saved {}/{} new posts from {}@{}", saved, statuses.size(), acct, instance);

        } catch (Exception e) {
            log.error("Failed to fetch from {}@{}: {}", acct, instance, e.getMessage());
        }
    }

    private boolean saveStatus(JsonNode status, Label mastodonLabel, Label sourceLabel, String sourceName) {
        if (!status.path("reblog").isNull()) return false;

        String externalPostId = status.path("id").asText(null);
        if (externalPostId == null) return false;
        if (postRepository.existsByExternalPostId(externalPostId)) return false;

        JsonNode account  = status.path("account");
        ExternalAuthor author = resolveOrCreateAuthor(account);

        CaptionResult captionResult = buildCaptionAndExtractUrls(status, sourceName);
        String        mediaRef      = extractMediaRef(status);
        LocalDateTime createdAt     = parseCreatedAt(status.path("created_at").asText());

        // Nur Posts mit Caption, Bild oder Source-URL speichern
        if (captionResult.caption().isBlank() && mediaRef == null && captionResult.urls().isEmpty()) {
            log.debug("Skipping post {} from {} — no caption, no image, no source URL", externalPostId, sourceName);
            return false;
        }

        Post post = Post.builder()
                .postId(UUID.randomUUID().toString())
                .externalPostId(externalPostId)
                .externalAuthor(author)
                .caption(captionResult.caption())
                .mediaRef(mediaRef)
                .isPublic(true)
                .isAiGenerated(false)
                .upvotes(0)
                .downvotes(0)
                .createdAt(createdAt)
                .build();

        // Labels hinzufügen
        addLabel(post, mastodonLabel);
        addLabel(post, sourceLabel);

        // Sources: eine Source-Entity mit je einer SingleSource pro extrahierter URL
        // Post hat @OneToOne Source — alle URLs kommen als SingleSources in eine Source
        List<String> urls = captionResult.urls();
        if (!urls.isEmpty()) {
            post.setSource(buildSource(urls));
        }

        // Topics aus Hashtags
        JsonNode tags = status.path("tags");
        if (tags.isArray()) {
            for (JsonNode tag : tags) {
                String tagName = tag.path("name").asText(null);
                if (tagName != null && !tagName.isBlank()) {
                    Topic topic = findOrCreateTopic(tagName);
                    addTopic(post, topic);
                }
            }
        }

        postRepository.save(post);
        return true;
    }

    private void addLabel(Post post, Label label) {
        PostLabelId id = new PostLabelId(post.getPostId(), label.getLabelId());
        PostLabel postLabel = PostLabel.builder()
                .id(id)
                .post(post)
                .label(label)
                .build();
        post.getPostLabels().add(postLabel);
    }

    private void addTopic(Post post, Topic topic) {
        PostTopicId id = new PostTopicId(post.getPostId(), topic.getTopicId());
        PostTopic postTopic = PostTopic.builder()
                .id(id)
                .post(post)
                .topic(topic)
                .build();
        post.getPostTopics().add(postTopic);
    }

    private Source buildSource(List<String> urls) {
        Source source = Source.builder()
                .sourceId(UUID.randomUUID().toString())
                .upvotes(0)
                .downvotes(0)
                .createdAt(LocalDateTime.now())
                .singleSources(new ArrayList<>())
                .build();

        for (String url : urls) {
            SingleSource singleSource = SingleSource.builder()
                    .singleSourceId(UUID.randomUUID().toString())
                    .url(url)
                    .source(source)
                    .build();
            source.getSingleSources().add(singleSource);
        }

        return sourceRepository.save(source);
    }

    private Topic findOrCreateTopic(String name) {
        String id = toId(name);
        return topicRepository.findById(id).orElseGet(() ->
                topicRepository.save(Topic.builder()
                        .topicId(id)
                        .name(name)
                        .emoji("🏷️")
                        .createdAt(LocalDateTime.now())
                        .build()));
    }

    private String toId(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "-");
    }

    private ExternalAuthor resolveOrCreateAuthor(JsonNode account) {
        String authorId = account.path("id").asText();
        return externalAuthorRepository.findByExternalAuthorId(authorId)
                .orElseGet(() -> externalAuthorRepository.save(
                        ExternalAuthor.builder()
                                .externalAuthorId(authorId)
                                .source(SOURCE_MASTODON)
                                .displayName(account.path("display_name").asText())
                                .avatarUrl(account.path("avatar").asText(null))
                                .profileUrl(account.path("url").asText(null))
                                .build()));
    }

    private String lookupAccountId(String instance, String acct) throws Exception {
        String url = "https://%s/api/v1/accounts/lookup?acct=%s".formatted(instance, acct);
        JsonNode response = get(url);
        return response != null ? response.path("id").asText(null) : null;
    }

    private List<JsonNode> fetchStatuses(String instance, String accountId) throws Exception {
        String url = "https://%s/api/v1/accounts/%s/statuses?limit=%d&exclude_replies=true"
                .formatted(instance, accountId, FETCH_LIMIT);
        JsonNode response = get(url);
        List<JsonNode> statuses = new ArrayList<>();
        if (response != null && response.isArray()) response.forEach(statuses::add);
        return statuses;
    }

    private record CaptionResult(String caption, List<String> urls) {}

    /**
     * Replicates the TypeScript FeedService caption logic exactly:
     * - strips HTML (without touching URLs)
     * - splits into lines, trims each
     * - extracts URL lines into sources (removes them from caption)
     * - applies source-specific filters (➡️ for Tagesschau, "Posted into" for BBC)
     * - Tagesschau:  title = lines[0], body = lines[1+], caption = title + "\n\n" + body (or just title)
     * - BBC News:    caption = all remaining lines joined by "\n"
     * - both fall back to card.title / card.description if nothing remains
     */
    private CaptionResult buildCaptionAndExtractUrls(JsonNode status, String sourceName) {
        String raw = stripHtml(status.path("content").asText(""));
        List<String> extractedUrls = new ArrayList<>();

        // Immediate card fallback when content is empty after stripping
        if (raw.isBlank()) {
            return cardFallback(status, extractedUrls);
        }

        List<String> lines = new ArrayList<>();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();

            // Collect URLs and skip the line
            Matcher m = URL_PATTERN.matcher(trimmed);
            if (m.find()) {
                m.reset();
                while (m.find()) extractedUrls.add(m.group());
                continue;
            }

            // Source-specific noise filters
            if ("Tagesschau".equals(sourceName) && trimmed.equals("➡️")) continue;
            if ("BBC News".equals(sourceName)   && trimmed.startsWith("Posted into")) continue;

            if (!trimmed.isBlank()) lines.add(trimmed);
        }

        String caption;
        if ("Tagesschau".equals(sourceName)) {
            String title = lines.isEmpty() ? "" : lines.get(0);
            String body  = lines.size() > 1
                    ? lines.subList(1, lines.size()).stream().collect(Collectors.joining("\n"))
                    : "";

            // Fallback to link-preview card when filtered lines left nothing
            if (title.isBlank()) {
                return cardFallback(status, extractedUrls);
            }
            caption = body.isBlank() ? title : title + "\n\n" + body;

        } else { // BBC News (and any future source)
            caption = lines.stream().collect(Collectors.joining("\n"));
            if (caption.isBlank()) {
                caption = stripHtml(status.path("card").path("title").asText(""));
            }
        }

        return new CaptionResult(caption, extractedUrls);
    }

    /** Builds caption from link-preview card (title + optional description). */
    private CaptionResult cardFallback(JsonNode status, List<String> alreadyExtractedUrls) {
        String title = stripHtml(status.path("card").path("title").asText(""));
        String desc  = stripHtml(status.path("card").path("description").asText(""));
        String caption = desc.isBlank() ? title : title + "\n\n" + desc;
        return new CaptionResult(caption, alreadyExtractedUrls);
    }

    private String extractMediaRef(JsonNode status) {
        JsonNode attachments = status.path("media_attachments");
        if (attachments.isArray()) {
            for (JsonNode media : attachments) {
                String type = media.path("type").asText();
                if ("image".equals(type) || "video".equals(type) || "gifv".equals(type)) {
                    return media.path("url").asText(null);
                }
            }
        }
        String cardImage = status.path("card").path("image").asText(null);
        return (cardImage != null && !cardImage.isBlank()) ? cardImage : null;
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        try { return OffsetDateTime.parse(createdAt).toLocalDateTime(); }
        catch (Exception e) { return LocalDateTime.now(); }
    }

    /** Matches the TypeScript stripHtml exactly: strips tags + HTML entities, trims. No URL removal. */
    private String stripHtml(String html) {
        return html
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
                .trim();
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) { log.warn("HTTP {} for {}", response.statusCode(), url); return null; }
        return objectMapper.readTree(response.body());
    }

    private String getExplanation(String sourceName) {
        return switch (sourceName) {
            case "Tagesschau" -> "Tagesschau is the main news program of ARD, Germany's public broadcasting network.";
            case "BBC News"   -> "BBC News is the internationally recognized news division of the British Broadcasting Corporation.";
            default           -> sourceName + " is a news source on Mastodon.";
        };
    }

    private Label findOrCreateLabel(String id, String name, String emoji, String explanation) {
        return labelRepository.findById(id).orElseGet(() ->
                labelRepository.save(Label.builder()
                        .labelId(id)
                        .name(name)
                        .emoji(emoji)
                        .explanation(explanation)
                        .build()));
    }
}