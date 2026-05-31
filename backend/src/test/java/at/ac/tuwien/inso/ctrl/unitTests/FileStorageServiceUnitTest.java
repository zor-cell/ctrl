package at.ac.tuwien.inso.ctrl.unitTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.file.FileStorageService;
import at.ac.tuwien.inso.ctrl.file.ImageProcessingService;
import at.ac.tuwien.inso.ctrl.file.VideoTranscodingService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
public class FileStorageServiceUnitTest {

  @Mock private S3Client s3Client;

  @Mock private S3Presigner s3Presigner;

  @Mock private S3Presigner internalS3Presigner;

  @Mock private VideoTranscodingService videoTranscodingService;

  private final ImageProcessingService imageProcessingService = new ImageProcessingService();

  private FileStorageService fileStorageService;

  @BeforeEach
  void setUp() {
    fileStorageService =
        new FileStorageService(
            s3Client,
            s3Presigner,
            internalS3Presigner,
            videoTranscodingService,
            imageProcessingService);
    ReflectionTestUtils.setField(fileStorageService, "bucketName", "ctrl-test-media");
    ReflectionTestUtils.setField(fileStorageService, "privateBucketName", "ctrl-test-private");
    ReflectionTestUtils.setField(fileStorageService, "publicBucketName", "ctrl-test-public");
    ReflectionTestUtils.setField(
        fileStorageService, "publicUrl", "http://localhost:8080/api/files/media-url?ref=");
  }

  @Test
  void uploadPostMedia_withValidImage_uploadsToPostMediaPrefix() {
    MockMultipartFile file =
        new MockMultipartFile("file", "picture.PNG", "image/png", new byte[] {1, 2, 3});

    FileStorageService.UploadedFile uploadedFile = fileStorageService.uploadPostMedia(file);

    PutObjectRequest request = capturePutObjectRequest();
    assertTrue(uploadedFile.mediaRef().startsWith("posts/media/"));
    assertTrue(uploadedFile.mediaRef().endsWith(".png"));
    assertEquals("image/png", uploadedFile.contentType());
    assertEquals(3L, uploadedFile.size());
    assertEquals("ctrl-test-private", request.bucket());
    assertEquals(uploadedFile.mediaRef(), request.key());
    assertEquals("image/png", request.contentType());
    verify(videoTranscodingService, never()).transcodeToMp4(any(), any());
  }

  @Test
  void uploadPostMedia_withQuickTimeVideo_transcodesAndUploadsMp4() throws Exception {
    MockMultipartFile file =
        new MockMultipartFile("file", "clip.mov", "video/quicktime", new byte[] {1, 2, 3});
    doAnswer(
            invocation -> {
              Path outputFile = invocation.getArgument(1);
              Files.write(outputFile, new byte[] {9, 8, 7, 6});
              return null;
            })
        .when(videoTranscodingService)
        .transcodeToMp4(any(Path.class), any(Path.class), anyInt());

    FileStorageService.UploadedFile uploadedFile = fileStorageService.uploadPostMedia(file);

    ArgumentCaptor<Path> inputCaptor = ArgumentCaptor.forClass(Path.class);
    ArgumentCaptor<Path> outputCaptor = ArgumentCaptor.forClass(Path.class);
    verify(videoTranscodingService)
        .transcodeToMp4(inputCaptor.capture(), outputCaptor.capture(), anyInt());
    PutObjectRequest request = capturePutObjectRequest();
    assertTrue(uploadedFile.mediaRef().startsWith("posts/media/"));
    assertTrue(uploadedFile.mediaRef().endsWith(".mp4"));
    assertEquals("video/mp4", uploadedFile.contentType());
    assertEquals(4L, uploadedFile.size());
    assertEquals(uploadedFile.mediaRef(), request.key());
    assertEquals("video/mp4", request.contentType());
    assertFalse(Files.exists(inputCaptor.getValue()));
    assertFalse(Files.exists(outputCaptor.getValue()));
  }

  @Test
  void uploadPostMedia_withEmptyFile_throwsInvalidRequestException() {
    MockMultipartFile file = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> fileStorageService.uploadPostMedia(file));

    assertEquals("File must not be empty", exception.getMessage());
    verifyNoInteractions(s3Client);
    verifyNoInteractions(videoTranscodingService);
  }

  @Test
  void uploadPostMedia_withUnsupportedContentType_throwsInvalidRequestException() {
    MockMultipartFile file =
        new MockMultipartFile("file", "notes.txt", "text/plain", new byte[] {1});

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> fileStorageService.uploadPostMedia(file));

    assertEquals("Only image and video files are supported", exception.getMessage());
    verifyNoInteractions(s3Client);
    verifyNoInteractions(videoTranscodingService);
  }

  @Test
  void uploadPostMedia_withTooLargeFile_throwsInvalidRequestException() {
    MultipartFile file = oversizedImage();

    InvalidRequestException exception =
        assertThrows(InvalidRequestException.class, () -> fileStorageService.uploadPostMedia(file));

    assertEquals("File must not be larger than 100 MB", exception.getMessage());
    verifyNoInteractions(s3Client);
    verifyNoInteractions(videoTranscodingService);
  }

  @Test
  void uploadProfilePicture_withValidImage_uploadsToProfilePicturePrefix() {
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.jpeg", "image/jpeg", new byte[] {1, 2});

    String mediaRef = fileStorageService.uploadProfilePicture(file);

    PutObjectRequest request = capturePutObjectRequest();
    assertTrue(mediaRef.startsWith("users/profile-pictures/"));
    assertTrue(mediaRef.endsWith(".jpeg"));
    assertEquals(mediaRef, request.key());
    assertEquals("ctrl-test-public", request.bucket());
    assertEquals("image/jpeg", request.contentType());
  }

  @Test
  void movePostMediaToPublicBucket_withPostMediaRef_copiesToPublicAndDeletesPrivate() {
    fileStorageService.movePostMediaToPublicBucket(
        "posts/media/11111111-1111-1111-1111-111111111111.png");

    ArgumentCaptor<CopyObjectRequest> copyCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
    verify(s3Client).copyObject(copyCaptor.capture());
    CopyObjectRequest mediaCopyRequest = copyCaptor.getValue();
    assertEquals("ctrl-test-private", mediaCopyRequest.sourceBucket());
    assertEquals(
        "posts/media/11111111-1111-1111-1111-111111111111.png", mediaCopyRequest.sourceKey());
    assertEquals("ctrl-test-public", mediaCopyRequest.destinationBucket());
    assertEquals(
        "posts/media/11111111-1111-1111-1111-111111111111.png", mediaCopyRequest.destinationKey());

    ArgumentCaptor<DeleteObjectRequest> deleteCaptor =
        ArgumentCaptor.forClass(DeleteObjectRequest.class);
    verify(s3Client).deleteObject(deleteCaptor.capture());
    assertEquals("ctrl-test-private", deleteCaptor.getValue().bucket());
    assertEquals(
        "posts/media/11111111-1111-1111-1111-111111111111.png", deleteCaptor.getValue().key());
  }

  @Test
  void movePostMediaToPublicBucket_withExternalUrl_doesNothing() {
    fileStorageService.movePostMediaToPublicBucket("https://mastodon.example/image.png");

    verifyNoInteractions(s3Client);
  }

  @Test
  void uploadProfilePicture_withVideo_throwsInvalidRequestException() {
    MockMultipartFile file =
        new MockMultipartFile("file", "avatar.mp4", "video/mp4", new byte[] {1, 2});

    InvalidRequestException exception =
        assertThrows(
            InvalidRequestException.class, () -> fileStorageService.uploadProfilePicture(file));

    assertEquals("Only image files are supported as profile picture", exception.getMessage());
    verifyNoInteractions(s3Client);
    verifyNoInteractions(videoTranscodingService);
  }

  @Test
  void resolveFileUrl_withMediaRef_returnsProtectedBackendUrl() {
    String mediaUrl = fileStorageService.resolveFileUrl("posts/media/file.png");

    assertEquals(
        "http://localhost:8080/api/files/media-url?ref=posts%2Fmedia%2Ffile.png", mediaUrl);
  }

  @Test
  void resolveFileUrl_withBlankMediaRef_returnsNull() {
    assertNull(fileStorageService.resolveFileUrl(null));
    assertNull(fileStorageService.resolveFileUrl("   "));
  }

  private PutObjectRequest capturePutObjectRequest() {
    ArgumentCaptor<PutObjectRequest> requestCaptor =
        ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
    return requestCaptor.getValue();
  }

  private MultipartFile oversizedImage() {
    MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
    when(file.isEmpty()).thenReturn(false);
    when(file.getSize()).thenReturn(100L * 1024L * 1024L + 1L);
    return file;
  }
}
