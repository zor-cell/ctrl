package at.ac.tuwien.inso.ctrl.feed.controller;

import at.ac.tuwien.inso.ctrl.feed.dto.response.FeedPostResponse;
import at.ac.tuwien.inso.ctrl.feed.service.FeedService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import at.ac.tuwien.inso.ctrl.post.scheduler.MastodonScheduler;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;
    private final MastodonScheduler mastodonScheduler;

    @GetMapping
    public ResponseEntity<List<FeedPostResponse>> getFeed(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int postsPerPage
    ) {
        return ResponseEntity.ok(feedService.getFeed(page, postsPerPage));
    }

    @PostMapping("/ingest") // test flow
    public ResponseEntity<String> triggerIngest() {
        mastodonScheduler.fetchMastodonPosts();
        return ResponseEntity.ok("Ingestion triggered");
    }
}