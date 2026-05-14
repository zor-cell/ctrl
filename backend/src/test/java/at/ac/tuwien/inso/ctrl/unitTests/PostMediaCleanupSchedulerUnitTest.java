package at.ac.tuwien.inso.ctrl.unitTests;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;

import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.file.PostMediaCleanupScheduler;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
public class PostMediaCleanupSchedulerUnitTest {

  @Mock private FileStorageService fileStorageService;

  @Mock private PostRepository postRepository;

  private PostMediaCleanupScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler = new PostMediaCleanupScheduler(fileStorageService, postRepository);
    ReflectionTestUtils.setField(scheduler, "orphanPostMediaMinAge", Duration.ofMinutes(30));
  }

  @Test
  void deleteOrphanPostMedia_withPrivateAndPublicOrphans_deletesOnlyUnreferencedMedia() {
    when(fileStorageService.listPostMediaRefsOlderThan(Duration.ofMinutes(30)))
        .thenReturn(List.of("posts/media/private-orphan.png", "posts/media/private-used.png"));
    when(fileStorageService.listPublicPostMediaRefsOlderThan(Duration.ofMinutes(30)))
        .thenReturn(List.of("posts/media/public-orphan.png", "posts/media/public-used.png"));
    when(postRepository.existsByMediaRef(anyString())).thenReturn(false);
    when(postRepository.existsByMediaRef("posts/media/private-used.png")).thenReturn(true);
    when(postRepository.existsByMediaRef("posts/media/public-used.png")).thenReturn(true);

    scheduler.deleteOrphanPostMedia();

    verify(fileStorageService).deletePostMedia("posts/media/private-orphan.png");
    verify(fileStorageService, never()).deletePostMedia("posts/media/private-used.png");
    verify(fileStorageService).deletePublicPostMedia("posts/media/public-orphan.png");
    verify(fileStorageService, never()).deletePublicPostMedia("posts/media/public-used.png");
  }
}
