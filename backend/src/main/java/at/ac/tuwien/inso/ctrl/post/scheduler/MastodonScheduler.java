package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.ExternalAuthorRepository;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient   httpClient   = HttpClient.newHttpClient();

    public MastodonScheduler(PostRepository postRepository,
                             ExternalAuthorRepository externalAuthorRepository) {
        this.postRepository           = postRepository;
        this.externalAuthorRepository = externalAuthorRepository;
    }

    // Läuft alle 15 Minuten
    @Scheduled(fixedRateString = "PT15M")
    @Transactional
    public void fetchMastodonPosts() {
        log.info("Mastodon scheduler started");
        fetchFromAccount("ard.social",       "tagesschau");
        fetchFromAccount("mastodon.social",  "BBCNews@flipboard.com");
        log.info("Mastodon scheduler finished");
    }

    private void fetchFromAccount(String instance, String acct) {
        try {
            // Schritt 1: Account-ID auflösen
            String accountId = lookupAccountId(instance, acct);
            if (accountId == null) {
                log.warn("Could not resolve account: {}@{}", acct, instance);
                return;
            }

            // Schritt 2: Statuses laden
            List<JsonNode> statuses = fetchStatuses(instance, accountId);

            // Schritt 3: Speichern
            int saved = 0;
            for (JsonNode status : statuses) {
                if (saveStatus(status)) saved++;
            }
            log.info("Saved {}/{} new posts from {}@{}", saved, statuses.size(), acct, instance);

        } catch (Exception e) {
            log.error("Failed to fetch from {}@{}: {}", acct, instance, e.getMessage());
        }
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
        if (response != null && response.isArray()) {
            response.forEach(statuses::add);
        }
        return statuses;
    }

    // Gibt true zurück wenn neu gespeichert, false wenn bereits vorhanden
    private boolean saveStatus(JsonNode status) {
        // Reblogs überspringen
        if (!status.path("reblog").isNull()) return false;

        String externalPostId = status.path("id").asText(null);
        if (externalPostId == null) return false;

        // Duplikate verhindern
        if (postRepository.existsByExternalPostId(externalPostId)) return false;

        JsonNode account = status.path("account");
        ExternalAuthor author = resolveOrCreateAuthor(account);

        String caption   = buildCaption(status);
        String mediaRef  = extractMediaRef(status);
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

        postRepository.save(post);
        return true;
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
                                .build()
                ));
    }

    private String buildCaption(JsonNode status) {
        String raw = stripHtml(status.path("content").asText(""));

        // Fallback auf Card-Title wenn Content leer
        if (raw.isBlank() && !status.path("card").isMissingNode()) {
            raw = status.path("card").path("title").asText("");
        }
        return raw.trim();
    }

    private String extractMediaRef(JsonNode status) {
        // Erst Media-Attachments prüfen
        JsonNode attachments = status.path("media_attachments");
        if (attachments.isArray()) {
            for (JsonNode media : attachments) {
                if ("image".equals(media.path("type").asText())) {
                    return media.path("url").asText(null);
                }
            }
        }
        // Dann Card-Image als Fallback
        String cardImage = status.path("card").path("image").asText(null);
        return (cardImage != null && !cardImage.isBlank()) ? cardImage : null;
    }

    private LocalDateTime parseCreatedAt(String createdAt) {
        try {
            return OffsetDateTime.parse(createdAt).toLocalDateTime();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String stripHtml(String html) {
        return html
                .replaceAll("<[^>]+>", "")       // HTML-Tags entfernen
                .replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&nbsp;", " ")
                .replaceAll("https?://\\S+", "") // URLs entfernen
                .replaceAll("\\s{2,}", "\n")     // Mehrfach-Leerzeilen reduzieren
                .trim();
    }

    private JsonNode get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("HTTP {} for {}", response.statusCode(), url);
            return null;
        }
        return objectMapper.readTree(response.body());
    }
}