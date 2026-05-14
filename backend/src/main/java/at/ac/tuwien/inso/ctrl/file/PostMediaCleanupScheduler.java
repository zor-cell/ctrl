package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PostMediaCleanupScheduler {
  private final FileStorageService fileStorageService;
  private final PostRepository postRepository;

  @Value("${files.cleanup.orphan-post-media.min-age:PT1H}")
  private Duration orphanPostMediaMinAge;

  @Scheduled(
      initialDelayString = "${files.cleanup.orphan-post-media.initial-delay:PT10M}",
      fixedDelayString = "${files.cleanup.orphan-post-media.fixed-delay:PT30M}")
  public void deleteOrphanPostMedia() {
    int deleted = 0;

    for (String mediaRef : fileStorageService.listPostMediaRefsOlderThan(orphanPostMediaMinAge)) {
      if (postRepository.existsByMediaRef(mediaRef)) {
        continue;
      }

      fileStorageService.deletePostMedia(mediaRef);
      deleted++;
    }

    for (String mediaRef :
        fileStorageService.listPublicPostMediaRefsOlderThan(orphanPostMediaMinAge)) {
      if (postRepository.existsByMediaRef(mediaRef)) {
        continue;
      }

      fileStorageService.deletePublicPostMedia(mediaRef);
      deleted++;
    }

    if (deleted > 0) {
      log.info("Deleted {} orphan post media files", deleted);
    }
  }
}
