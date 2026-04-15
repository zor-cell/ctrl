package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.post.entity.*;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.LabelRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class MastodonScheduler {

    private static final String SOURCE_MASTODON = "mastodon";
    private static final int    FETCH_LIMIT      = 20;

    private final PostRepository           postRepository;
    private final ExternalAuthorRepository externalAuthorRepository;
    private final LabelRepository          labelRepository;
    private final TopicRepository          topicRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    public MastodonScheduler(PostRepository postRepository,
                             ExternalAuthorRepository externalAuthorRepository,
                             LabelRepository labelRepository,
                             TopicRepository topicRepository) {
        this.postRepository           = postRepository;
        this.externalAuthorRepository = externalAuthorRepository;
        this.labelRepository          = labelRepository;
        this.topicRepository          = topicRepository;
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
                if (saveStatus(status, mastodonLabel, sourceLabel)) saved++;
            }
            log.info("Saved {}/{} new posts from {}@{}", saved, statuses.size(), acct, instance);

        } catch (Exception e) {
            log.error("Failed to fetch from {}@{}: {}", acct, instance, e.getMessage());
        }
    }

    private boolean saveStatus(JsonNode status, Label mastodonLabel, Label sourceLabel) {
        if (!status.path("reblog").isNull()) return false;

        String externalPostId = status.path("id").asText(null);
        if (externalPostId == null) return false;
        if (postRepository.existsByExternalPostId(externalPostId)) return false;

        JsonNode account  = status.path("account");
        ExternalAuthor author = resolveOrCreateAuthor(account);

        String        caption   = buildCaption(status);
        String        mediaRef  = extractMediaRef(status);
        LocalDateTime createdAt = parseCreatedAt(status.path("created_at").asText());

        Post post = Post.builder()
                .postId(UUID.randomUUID().toString())
                .externalPostId(externalPostId)
                .externalAuthor(author)
                .caption(caption)
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

    private String buildCaption(JsonNode status) {
        String raw = stripHtml(status.path("content").asText(""));
        if (raw.isBlank() && !status.path("card").isMissingNode()) {
            raw = status.path("card").path("title").asText("");
        }
        return raw.trim();
    }

    private String extractMediaRef(JsonNode status) {
        JsonNode attachments = status.path("media_attachments");
        if (attachments.isArray()) {
            for (JsonNode media : attachments) {
                if ("image".equals(media.path("type").asText())) {
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

    private String stripHtml(String html) {
        return html
                .replaceAll("<[^>]+>", "")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ")
                .replaceAll("https?://\\S+", "")
                .replaceAll("\\s{2,}", "\n")
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