package at.ac.tuwien.inso.ctrl.unitTests;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.file.FileController;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        .thenReturn("http://localhost:8080/api/files/media?ref=posts%2Fmedia%2Ffile.png");

    FileUploadResponse response = fileController.uploadPostMedia(file);

    assertEquals("posts/media/file.png", response.mediaRef());
    assertEquals(
        "http://localhost:8080/api/files/media?ref=posts%2Fmedia%2Ffile.png",
        response.mediaUrl());
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

  @Test
  void getMedia_ShouldReturnStoredFileContent() {
    byte[] content = new byte[] {1, 2, 3};
    FileStorageService.StoredFile storedFile =
        new FileStorageService.StoredFile(content, "image/png", content.length);

    when(fileStorageService.loadFile("posts/media/file.png")).thenReturn(storedFile);

    ResponseEntity<byte[]> response = fileController.getMedia("posts/media/file.png");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(MediaType.IMAGE_PNG, response.getHeaders().getContentType());
    assertEquals(content.length, response.getHeaders().getContentLength());
    assertArrayEquals(content, response.getBody());
    assertNotNull(response.getHeaders().getCacheControl());
    verify(fileStorageService).loadFile("posts/media/file.png");
  }

  @Test
  void getMedia_ShouldUseOctetStream_WhenContentTypeIsMissing() {
    byte[] content = new byte[] {1, 2, 3};
    FileStorageService.StoredFile storedFile =
        new FileStorageService.StoredFile(content, null, content.length);

    when(fileStorageService.loadFile("posts/media/file")).thenReturn(storedFile);

    ResponseEntity<byte[]> response = fileController.getMedia("posts/media/file");

    assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaders().getContentType());
    assertArrayEquals(content, response.getBody());
  }
}
