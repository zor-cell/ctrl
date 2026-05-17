package at.ac.tuwien.inso.ctrl.post.scheduler;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Every 15 minutes: ┌─────────────────────────────────────────────┐ │ For each account in ACCOUNTS:
 * │ │ │ │ 1. Resolve account ID via Mastodon API │ │ ↓ │ │ 2. Fetch latest posts (max FETCH_LIMIT)
 * │ │ ↓ │ │ 3. Filter duplicates & boosts │ │ ↓ │ │ 4. Build caption: │ │ • named accounts →
 * source-specific │ │ formatting (Tagesschau / BBC) │ │ • all others → strip links only │ │ ↓ │ │
 * 5. Assign label "Mastodon" and save │ │ (Topics are handled by │ │ TopicAssignmentScheduler
 * separately) │ └─────────────────────────────────────────────┘
 */
@Slf4j
@Component
@ConditionalOnProperty(
    value = "spring.task.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class MastodonScheduler {

  private static final String SOURCE_MASTODON = "mastodon";
  private static final int FETCH_LIMIT = 30;
  private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+");

  /**
   * Central list of Mastodon accounts to fetch from.
   *
   * <p>Fields:
   *
   * <ul>
   *   <li>{@code instance} – Mastodon instance hostname (e.g. "mastodon.social")
   *   <li>{@code acct} – account handle on that instance
   *   <li>{@code sourceName} – non-null activates source-specific caption formatting; null →
   *       generic link-strip only
   * </ul>
   *
   * To add a plain account just provide null as sourceName:
   *
   * <pre>
   * new AccountConfig("mastodon.social", "someuser", null)
   * </pre>
   */
  private static final List<AccountConfig> ACCOUNTS =
      List.of(
          new AccountConfig("ard.social", "tagesschau", "Tagesschau"),
          new AccountConfig("mastodon.social", "BBCNews@flipboard.com", "BBC News"),
          // ── add more accounts below ──────────────────────────────────────────
          new AccountConfig("mastodon.social", "Techcrunch@flipboard.com", null),
          new AccountConfig("mastodon.social", "instyle@flipboard.com", null),
          new AccountConfig("mastodon.social", "Salon@flipboard.com", null),
          new AccountConfig("mastodon.social", "people@flipboard.com", null),
          new AccountConfig("mastodon.social", "globeandmail@flipboard.com", null),
          new AccountConfig("mastodon.social", "skeletor@mas.to", null),
          new AccountConfig("mastodon.social", "TheEconomist@flipboard.com", null),
          new AccountConfig("mastodon.social", "9to5mac@flipboard.com", null),
          new AccountConfig("mastodon.social", "macrumors@flipboard.com", null),
          new AccountConfig("mastodon.social", "randahl", null),
          new AccountConfig("universeodon.com", "georgetakei", null),
          new AccountConfig("pixelfed.social", "pixelfed", null),
          new AccountConfig("masto.ai", "rbreich", null),
          new AccountConfig("mastodon.social", "neilhimself", null),
          new AccountConfig("social.growyourown.services", "FediTips", null),
          new AccountConfig("mastodon.social", "arstechnica", null),
          new AccountConfig("edi.social", "janboehm", null),
          new AccountConfig("mastodon.social", "taylorlorenz", null),
          new AccountConfig("newsie.social", "ProPublica", null),
          new AccountConfig("antisocial.ly", "primalmotion", null),
          new AccountConfig("mastodon.social", "derpostillon", null),
          new AccountConfig("universeodon.com", "godpod", null),
          new AccountConfig("ec.social-network.europa.eu", "EUCommission", null),
          new AccountConfig("simkey.net", "_kokt", null),
          new AccountConfig("hachyderm.io", "molly0xfff", null),
          new AccountConfig("digitalcourage.social", "Volksverpetzer", null),
          new AccountConfig("mastodon.social", "nixCraft", null),
          new AccountConfig("mastodon.social", "warandpeas", null),
          new AccountConfig("tapbots.social", "ivory", null),
          new AccountConfig("mastodon.world", "auschwitzmuseum", null),
          new AccountConfig("infosec.exchange", "briankrebs", null),
          new AccountConfig("mediapart.social", "mediapart", null),
          new AccountConfig("mastodonapp.uk", "stephenfry", null),
          new AccountConfig("mastodon.nu", "gretathunberg", null),
          new AccountConfig("mastodon.social", "elhotzo", null),
          new AccountConfig("troet.cafe", "ralphruthe", null),
          new AccountConfig("tapbots.social", "tapbots", null),
          new AccountConfig("mastodon.archive.org", "internetarchive", null),
          new AccountConfig("infosec.exchange", "SwiftOnSecurity", null),
          new AccountConfig("mastodon.social", "workingclasshistory", null),
          new AccountConfig("mastodon.social", "QasimRashid", null),
          new AccountConfig("mastodon.social", "tazgetroete", null),
          new AccountConfig("mastodon.social", "gamingonlinux", null),
          new AccountConfig("edi.social", "zdfmagazin", null),
          new AccountConfig("tkz.one", "tkz", null),
          new AccountConfig("social.heise.de", "heiseonline", null),
          new AccountConfig("infosec.exchange", "malwaretech", null),
          new AccountConfig("mastodon.social", "MrLovenstein", null),
          new AccountConfig("hachyderm.io", "evacide", null),
          new AccountConfig("chaos.social", "afelia", null),
          new AccountConfig("mastodon.world", "signalapp", null),
          new AccountConfig("social.beachcom.org", "nasa", null),
          new AccountConfig("gram.social", "Alice", null),
          new AccountConfig("social.growyourown.services", "FediFollows", null),
          new AccountConfig("mastodon.social", "drewharwell", null),
          new AccountConfig("mstdn.social", "lowqualityfacts", null),
          new AccountConfig("gram.social", "stux", null),
          new AccountConfig("pixelfed.social", "dansup", null),
          new AccountConfig("mastodon.social", "elonjet", null),
          new AccountConfig("reentry.codl.fr", "apod", null),
          new AccountConfig("cyberplace.social", "GossiTheDog", null),
          new AccountConfig("mastodon.social", "Sheril", null),
          new AccountConfig("mastodon.social", "gutenberg_org", null),
          new AccountConfig("mastodon.de", "ErikUden", null),
          new AccountConfig("mastodon.social", "jeffjarvis", null),
          new AccountConfig("mastodon.social", "emptywheel", null),
          new AccountConfig("mastodon.social", "wonderofscience", null),
          new AccountConfig("chaos.social", "fff", null),
          new AccountConfig("mastodon.social", "torproject", null),
          new AccountConfig("mastodon.art", "Curator", null),
          new AccountConfig("journa.host", "anneapplebaum", null),
          new AccountConfig("mastodon.online", "SocraticEthics", null),
          new AccountConfig("mastodon.social", "oatmeal", null),
          new AccountConfig("mastodon.social", "lisamelton", null),
          new AccountConfig("mamot.fr", "LaQuadrature", null),
          new AccountConfig("infosec.exchange", "micahflee", null),
          new AccountConfig("mastodon.social", "gruber", null),
          new AccountConfig("mastodon.social", "AbandonedAmerica", null),
          new AccountConfig("mastodon.social", "lemonde", null),
          new AccountConfig("chaos.social", "netzpolitik_feed", null),
          new AccountConfig("fediscience.org", "ct_bergstrom", null),
          new AccountConfig("mstdn.social", "jentaub", null),
          new AccountConfig("flipboard.social", "newsguyusa", null),
          new AccountConfig("mastodon.social", "marcoarment", null),
          new AccountConfig("gram.social", "Gram", null),
          new AccountConfig("cultur.social", "marcuwekling", null),
          new AccountConfig("mastodon.social", "nocontexttrek", null),
          new AccountConfig("mastodon.green", "VQuaschning", null),
          new AccountConfig("social.jvns.ca", "b0rk", null),
          new AccountConfig("infosec.exchange", "hacks4pancakes", null),
          new AccountConfig("mas.to", "TechConnectify", null),
          new AccountConfig("mastodon.social", "protonprivacy", null),
          new AccountConfig("omaka.nr1a.inc", "omakano", null),
          new AccountConfig("mastodon.social", "caseynewton", null),
          new AccountConfig("digitalcourage.social", "digitalcourage", null),
          new AccountConfig("social.bund.de", "bfdi", null),
          new AccountConfig("mastodon.social", "bagder", null),
          new AccountConfig("mastodon.ar.al", "aral", null),
          new AccountConfig("mastodon.social", "Teri_Kanefield", null),
          new AccountConfig("mastodon.social", "sarahbosetti", null),
          new AccountConfig("social.kernel.org", "torvalds", null),
          new AccountConfig("framapiaf.org", "Framasoft", null),
          new AccountConfig("mastodon.social", "Tutanota", null),
          new AccountConfig("fediscience.org", "rahmstorf", null),
          new AccountConfig("mastodon.social", "itsfoss", null),
          new AccountConfig("mastodon.social", "ioerror", null),
          new AccountConfig("mastodon.social", "AOC", null),
          new AccountConfig("mastodon.social", "medeabenjamin", null),
          new AccountConfig("infosec.exchange", "campuscodi", null),
          new AccountConfig("mastodon.social", "feditips", null),
          new AccountConfig("hachyderm.io", "jesslynnrose", null),
          new AccountConfig("mastodon.social", "theconversationus", null),
          new AccountConfig("social.tchncs.de", "kuketzblog", null),
          new AccountConfig("mastodon.social", "pluralistic", null),
          new AccountConfig("mastodon.online", "atomicpoet", null),
          new AccountConfig("mastodon.social", "mekkaokereke", null),
          new AccountConfig("mastodon.social", "bookstodon", null),
          new AccountConfig("mastodon.social", "crookedmedia", null),
          new AccountConfig("mastodon.social", "RnaudBertrand", null),
          new AccountConfig("infosec.exchange", "malwareunicorn", null),
          new AccountConfig("mastodon.social", "jayrayner", null),
          new AccountConfig("mastodon.social", "sciencegirl", null),
          new AccountConfig("mastodon.social", "owillis", null),
          new AccountConfig("social.linux.pizza", "drewdevault", null),
          new AccountConfig("mastodon.social", "wikimedia", null),
          new AccountConfig("mastodon.social", "mollyjongfast", null),
          new AccountConfig("mastodon.social", "AsteadWesley", null),
          new AccountConfig("mastodon.social", "feditips_bot", null),
          new AccountConfig("journa.host", "mehdirhasan", null),
          new AccountConfig("mastodon.social", "gizmodo", null),
          new AccountConfig("mastodon.social", "MonaApp", null),
          new AccountConfig("mastodon.social", "doctorow", null),
          new AccountConfig("mastodon.social", "RVAwonk", null),
          new AccountConfig("mastodon.social", "AnarchoNina", null),
          new AccountConfig("mastodon.social", "democracynow", null),
          new AccountConfig("mastodon.social", "thenation", null),
          new AccountConfig("mastodon.social", "404mediaco", null),
          new AccountConfig("mastodon.social", "SDF", null),
          new AccountConfig("mastodon.social", "effie", null),
          new AccountConfig("mastodon.social", "Bellingcat", null),
          new AccountConfig("mastodon.social", "joshtpm", null),
          new AccountConfig("mastodon.social", "themarkup", null),
          new AccountConfig("hachyderm.io", "stroughtonsmith", null),
          new AccountConfig("mastodon.social", "danielpunkass", null),
          new AccountConfig("mastodon.social", "YourAnonCentral", null),
          new AccountConfig("mastodon.social", "daringfireball", null),
          new AccountConfig("mastodon.social", "markgurman", null),
          new AccountConfig("mastodon.social", "tabletop", null),
          new AccountConfig("mastodon.social", "cabel", null),
          new AccountConfig("mastodon.social", "Gargron", null),
          new AccountConfig("mastodon.social", "kurzgesagt", null),
          new AccountConfig("mastodon.social", "AriMelber", null),
          new AccountConfig("mastodon.social", "MastodonMigration", null),
          new AccountConfig("mastodon.social", "OpenStreetMap", null),
          new AccountConfig("mastodon.social", "atrupar", null),
          new AccountConfig("mastodon.social", "TheAtlantic", null),
          new AccountConfig("mastodon.social", "Reuters", null),
          new AccountConfig("mastodon.social", "RiotFest", null),
          new AccountConfig("mastodon.social", "joelockhart", null),
          new AccountConfig("mastodon.social", "kottke", null),
          new AccountConfig("mastodon.social", "scholar", null),
          new AccountConfig("mastodon.social", "AnilDash", null),
          new AccountConfig("mastodon.social", "NPR", null),
          new AccountConfig("mastodon.social", "nprpolitics", null),
          new AccountConfig("mastodon.social", "ClimateNexus", null),
          new AccountConfig("mastodon.social", "Futurism", null),
          new AccountConfig("mastodon.social", "KenWhite", null),
          new AccountConfig("mastodon.social", "OpenSourceOrg", null),
          new AccountConfig("mastodon.social", "Cernovich", null),
          new AccountConfig("mastodon.social", "snopes", null),
          new AccountConfig("mastodon.social", "NWS", null),
          new AccountConfig("mastodon.social", "TheOnion", null),
          new AccountConfig("mastodon.social", "EFFUpdates", null),
          new AccountConfig("mastodon.social", "jwz", null),
          new AccountConfig("mastodon.social", "washingtonpost", null),
          new AccountConfig("mastodon.social", "MKBHD", null),
          new AccountConfig("mastodon.social", "verge", null),
          new AccountConfig("mastodon.social", "TechDirt", null),
          new AccountConfig("mastodon.social", "Jason", null),
          new AccountConfig("mastodon.social", "joshua", null),
          new AccountConfig("mastodon.social", "404media", null),
          new AccountConfig("mastodon.social", "brianklaas", null),
          new AccountConfig("mastodon.social", "samthielman", null),
          new AccountConfig("mastodon.social", "briantylercohen", null),
          new AccountConfig("mastodon.social", "charliewarzel", null),
          new AccountConfig("mastodon.social", "wired", null),
          new AccountConfig("mastodon.social", "eff", null),
          new AccountConfig("mastodon.social", "theintercept", null),
          new AccountConfig("mastodon.social", "mollycrabapple", null),
          new AccountConfig("mastodon.social", "Snowden", null),
          new AccountConfig("mastodon.social", "AmandaMarcotte", null),
          new AccountConfig("mastodon.social", "jayrosen_nyu", null),
          new AccountConfig("mastodon.social", "Mastodon", null),
          new AccountConfig("mastodon.social", "dangillmor", null),
          new AccountConfig("mastodon.social", "RudePundit", null),
          new AccountConfig("mastodon.social", "atpfm", null),
          new AccountConfig("mastodon.online", "NatureMC", null),
          new AccountConfig("mas.to", "natureworks", null),
          new AccountConfig("ecoevo.social", "BathNature", null),
          new AccountConfig("pixelfed.social", "pixel.fabian", null),
          new AccountConfig("zirk.us", "FilmNoirPosters", null),
          new AccountConfig("mastodonapp.uk", "horroryear", null),
          new AccountConfig("transportation.social", "bikepedantic", null),
          new AccountConfig("sportsfeed.me", "nfl", null),
          new AccountConfig("mastodon.social", "larstransportworld", null),
          new AccountConfig("infosec.exchange", "catsalad", null),
          new AccountConfig("mas.to", "SmudgeTheInsultCat", null),
          new AccountConfig("mastodon.social", "grimalkina", null),
          new AccountConfig("mastodon.social", "cmconseils", null),
          new AccountConfig("mastodon.social", "campuscodi", null),
          new AccountConfig("mas.to", "SmudgeTheInsultCat", null),
          new AccountConfig("mastodon.social", "sundogplanets", null),
          new AccountConfig("mastodon.social", "hotdogsladies", null),
          new AccountConfig("aus.social", "firstdogonthemoon", null),
          new AccountConfig("botsin.space", "dogsofmastodon", null));

  /** Carries the per-account fetch configuration. */
  private record AccountConfig(String instance, String acct, String sourceName) {}

  // ── dependencies ─────────────────────────────────────────────────────────

  private final PostRepository postRepository;
  private final ExternalAuthorRepository externalAuthorRepository;
  private final LabelRepository labelRepository;
  private final SourceRepository sourceRepository;
  private final EmbeddingService embeddingService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  public MastodonScheduler(
      PostRepository postRepository,
      ExternalAuthorRepository externalAuthorRepository,
      LabelRepository labelRepository,
      SourceRepository sourceRepository,
      EmbeddingService embeddingService) {
    this.postRepository = postRepository;
    this.externalAuthorRepository = externalAuthorRepository;
    this.labelRepository = labelRepository;
    this.sourceRepository = sourceRepository;
    this.embeddingService = embeddingService;
  }

  // ── scheduler entry point ─────────────────────────────────────────────────

  @Scheduled(fixedRateString = "PT15M")
  public void fetchMastodonPosts() {
    log.info("Mastodon scheduler started — {} account(s) configured", ACCOUNTS.size());

    // The single label assigned to every post fetched here
    Label mastodonLabel = findOrCreateMastodonLabel();

    for (AccountConfig account : ACCOUNTS) {
      fetchFromAccount(account, mastodonLabel);
    }

    log.info("Mastodon scheduler finished");
  }

  @Transactional
  public Label findOrCreateMastodonLabel() {
    return findOrCreateLabel(
        "mastodon",
        "Mastodon",
        "🐘",
        "Mastodon is a decentralized social media platform where this post was originally"
            + " published.");
  }

  // ── per-account fetch ─────────────────────────────────────────────────────

  @Transactional
  public void fetchFromAccount(AccountConfig account, Label mastodonLabel) {
    try {
      String accountId = lookupAccountId(account.instance(), account.acct());
      if (accountId == null) {
        log.warn("Could not resolve account: {}@{}", account.acct(), account.instance());
        return;
      }

      List<JsonNode> statuses = fetchStatuses(account.instance(), accountId);

      int saved = 0;
      for (JsonNode status : statuses) {
        if (saveStatus(status, mastodonLabel, account.sourceName())) saved++;
      }

      log.info(
          "Saved {}/{} new posts from {}@{}",
          saved,
          statuses.size(),
          account.acct(),
          account.instance());

    } catch (Exception e) {
      log.error(
          "Failed to fetch from {}@{}: {}", account.acct(), account.instance(), e.getMessage());
    }
  }

  // ── post persistence ──────────────────────────────────────────────────────

  /**
   * @param sourceName non-null → use source-specific caption formatting; null → strip links only
   *     (generic accounts)
   */
  private boolean saveStatus(JsonNode status, Label mastodonLabel, String sourceName) {
    if (!status.path("reblog").isNull()) return false;

    String externalPostId = status.path("id").asText(null);
    if (externalPostId == null) return false;
    if (postRepository.existsByExternalPostId(externalPostId)) return false;

    JsonNode account = status.path("account");
    ExternalAuthor author = resolveOrCreateAuthor(account);

    CaptionResult captionResult = buildCaption(status, sourceName);
    String mediaRef = extractMediaRef(status);
    LocalDateTime createdAt = parseCreatedAt(status.path("created_at").asText());

    // Only persist posts that have at least some content
    if (captionResult.caption().isBlank() && mediaRef == null && captionResult.urls().isEmpty()) {
      log.debug("Skipping post {} — no caption, no image, no source URL", externalPostId);
      return false;
    }

    float[] embedding = embeddingService.embedTextWithImage(captionResult.caption(), mediaRef);

    Post post =
        Post.builder()
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
            .embedding(embedding)
            .build();

    addLabel(post, mastodonLabel);

    List<String> urls = captionResult.urls();
    if (!urls.isEmpty()) {
      post.setSource(buildSource(urls));
    }

    // Topics are intentionally NOT assigned here.
    // TopicAssignmentScheduler picks up posts without topics and assigns them via
    // pgvector.

    postRepository.save(post);
    return true;
  }

  // ── caption building ──────────────────────────────────────────────────────

  private record CaptionResult(String caption, List<String> urls) {}

  /**
   * Dispatches to the appropriate caption strategy based on {@code sourceName}.
   *
   * <ul>
   *   <li>"Tagesschau" / "BBC News" → source-specific formatting (title/body split, noise filters)
   *   <li>{@code null} → generic: strip HTML, remove URL lines, join remaining text
   * </ul>
   */
  private CaptionResult buildCaption(JsonNode status, String sourceName) {
    if (sourceName != null) {
      return buildFormattedCaption(status, sourceName);
    }
    return buildGenericCaption(status);
  }

  /**
   * Source-specific caption logic for named accounts (Tagesschau, BBC News). Strips HTML, extracts
   * URLs into sources, applies per-source noise filters, and structures the caption (title + body
   * for Tagesschau; joined lines for BBC).
   */
  private CaptionResult buildFormattedCaption(JsonNode status, String sourceName) {
    String raw = stripHtml(status.path("content").asText(""));
    List<String> extractedUrls = new ArrayList<>();

    if (raw.isBlank()) {
      return cardFallback(status, extractedUrls);
    }

    List<String> lines = new ArrayList<>();
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();

      Matcher m = URL_PATTERN.matcher(trimmed);
      if (m.find()) {
        m.reset();
        while (m.find()) extractedUrls.add(m.group());
        continue;
      }

      if ("Tagesschau".equals(sourceName) && trimmed.equals("➡️")) continue;
      if ("BBC News".equals(sourceName) && trimmed.startsWith("Posted into")) continue;

      if (!trimmed.isBlank()) lines.add(trimmed);
    }

    String caption;
    if ("Tagesschau".equals(sourceName)) {
      String title = lines.isEmpty() ? "" : lines.get(0);
      String body =
          lines.size() > 1
              ? lines.subList(1, lines.size()).stream().collect(Collectors.joining("\n"))
              : "";
      if (title.isBlank()) {
        return cardFallback(status, extractedUrls);
      }
      caption = body.isBlank() ? title : title + "\n\n" + body;
    } else {
      // BBC News and any future named source
      caption = lines.stream().collect(Collectors.joining("\n"));
      if (caption.isBlank()) {
        caption = stripHtml(status.path("card").path("title").asText(""));
      }
    }

    return new CaptionResult(caption, extractedUrls);
  }

  /**
   * Generic caption builder for plain Mastodon accounts. URLs are extracted into sources and
   * removed inline from the text — the surrounding caption text on the same line is preserved. No
   * structural reformatting is applied.
   */
  private CaptionResult buildGenericCaption(JsonNode status) {
    String raw = stripHtml(status.path("content").asText(""));
    List<String> extractedUrls = new ArrayList<>();

    if (raw.isBlank()) {
      return cardFallback(status, extractedUrls);
    }

    List<String> lines = new ArrayList<>();
    for (String line : raw.split("\n")) {
      String trimmed = line.trim();

      // Extract every URL from the line into sources,
      // then keep whatever text remains on that line.
      Matcher m = URL_PATTERN.matcher(trimmed);
      StringBuffer sb = new StringBuffer();
      while (m.find()) {
        extractedUrls.add(m.group());
        m.appendReplacement(sb, "");
      }
      m.appendTail(sb);

      String remaining = sb.toString().trim();
      if (!remaining.isBlank()) lines.add(remaining);
    }

    String caption = lines.stream().collect(Collectors.joining("\n"));
    return new CaptionResult(caption, extractedUrls);
  }

  /** Builds caption from the link-preview card (title and optional description). */
  private CaptionResult cardFallback(JsonNode status, List<String> alreadyExtractedUrls) {
    String title = stripHtml(status.path("card").path("title").asText(""));
    String desc = stripHtml(status.path("card").path("description").asText(""));
    String caption = desc.isBlank() ? title : title + "\n\n" + desc;
    return new CaptionResult(caption, alreadyExtractedUrls);
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private void addLabel(Post post, Label label) {
    PostLabelId id = new PostLabelId(post.getPostId(), label.getLabelId());
    PostLabel postLabel = PostLabel.builder().id(id).post(post).label(label).build();
    post.getPostLabels().add(postLabel);
  }

  private Source buildSource(List<String> urls) {
    Source source =
        Source.builder()
            .sourceId(UUID.randomUUID().toString())
            .upvotes(0)
            .downvotes(0)
            .createdAt(LocalDateTime.now())
            .singleSources(new ArrayList<>())
            .build();

    for (String url : urls) {
      SingleSource singleSource =
          SingleSource.builder()
              .singleSourceId(UUID.randomUUID().toString())
              .url(url)
              .source(source)
              .build();
      source.getSingleSources().add(singleSource);
    }

    return sourceRepository.save(source);
  }

  private ExternalAuthor resolveOrCreateAuthor(JsonNode account) {
    String authorId = account.path("id").asText();
    return externalAuthorRepository
        .findByExternalAuthorId(authorId)
        .orElseGet(
            () ->
                externalAuthorRepository.save(
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
    String url =
        "https://%s/api/v1/accounts/%s/statuses?limit=%d&exclude_replies=true"
            .formatted(instance, accountId, FETCH_LIMIT);
    JsonNode response = get(url);
    List<JsonNode> statuses = new ArrayList<>();
    if (response != null && response.isArray()) response.forEach(statuses::add);
    return statuses;
  }

  private LocalDateTime parseCreatedAt(String createdAt) {
    try {
      return OffsetDateTime.parse(createdAt).toLocalDateTime();
    } catch (Exception e) {
      return LocalDateTime.now();
    }
  }

  /**
   * Strips HTML tags and common entities. Does not remove URLs from the text (URL extraction is
   * handled separately in the caption builders).
   */
  private String stripHtml(String html) {
    return html.replaceAll("<[^>]+>", "")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&nbsp;", " ")
        .trim();
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

  private JsonNode get(String url) throws Exception {
    HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      log.warn("HTTP {} for {}", response.statusCode(), url);
      return null;
    }
    return objectMapper.readTree(response.body());
  }

  private Label findOrCreateLabel(String id, String name, String emoji, String explanation) {
    return labelRepository
        .findById(id)
        .orElseGet(
            () ->
                labelRepository.save(
                    Label.builder()
                        .labelId(id)
                        .name(name)
                        .emoji(emoji)
                        .explanation(explanation)
                        .build()));
  }
}
