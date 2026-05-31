package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.file.FileController;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
public class FileControllerUnitTest {

  @Mock private FileStorageService fileStorageService;

  @Mock private MultipartFile file;

  @Mock private PostRepository postRepository;

  @Mock private UserRepository userRepository;

  @Mock private FriendRelationRepository friendRelationRepository;

  @InjectMocks private FileController fileController;

  @Test
  void uploadPostMedia_ShouldReturnUploadResponse_WhenFileIsStored() {
    FileStorageService.UploadedFile uploadedFile =
        new FileStorageService.UploadedFile("posts/media/file.png", "image/png", 1234L);

    when(fileStorageService.uploadPostMedia(file)).thenReturn(uploadedFile);

    FileUploadResponse response = fileController.uploadPostMedia(file);

    assertEquals("posts/media/file.png", response.mediaRef());
    assertEquals("image/png", response.contentType());
    assertEquals(1234L, response.size());
    verify(fileStorageService).uploadPostMedia(file);
  }

  @Test
  void uploadPostMedia_ShouldReturnMp4Response_WhenVideoIsStored() {
    FileStorageService.UploadedFile uploadedFile =
        new FileStorageService.UploadedFile("posts/media/video.mp4", "video/mp4", 9876L);

    when(fileStorageService.uploadPostMedia(file)).thenReturn(uploadedFile);

    FileUploadResponse response = fileController.uploadPostMedia(file);

    assertEquals("posts/media/video.mp4", response.mediaRef());
    assertEquals("video/mp4", response.contentType());
    assertEquals(9876L, response.size());
    verify(fileStorageService).uploadPostMedia(file);
  }
}
