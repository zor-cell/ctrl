package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.file.FileController;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
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

  @InjectMocks private FileController fileController;

  @Test
  void uploadPostMedia_ShouldReturnUploadResponse_WhenFileIsStored() {
    FileStorageService.UploadedFile uploadedFile =
        new FileStorageService.UploadedFile("posts/media/file.png", "image/png", 1234L);

    when(fileStorageService.uploadPostMedia(file)).thenReturn(uploadedFile);
    when(fileStorageService.resolveFileUrl("posts/media/file.png"))
        .thenReturn("http://localhost:3900/ctrl-dev-media/posts/media/file.png");

    FileUploadResponse response = fileController.uploadPostMedia(file);

    assertEquals("posts/media/file.png", response.mediaRef());
    assertEquals("http://localhost:3900/ctrl-dev-media/posts/media/file.png", response.mediaUrl());
    assertEquals("image/png", response.contentType());
    assertEquals(1234L, response.size());
    verify(fileStorageService).uploadPostMedia(file);
    verify(fileStorageService).resolveFileUrl("posts/media/file.png");
  }

  @Test
  void uploadPostMedia_ShouldPropagateException_WhenStorageRejectsFile() {
    when(fileStorageService.uploadPostMedia(file))
        .thenThrow(new InvalidRequestException("Only image and video files are supported"));

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> fileController.uploadPostMedia(file));

    assertEquals("Only image and video files are supported", exception.getMessage());
  }
}
