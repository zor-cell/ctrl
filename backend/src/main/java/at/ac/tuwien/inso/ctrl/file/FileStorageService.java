package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.exception.ResourceNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class FileStorageService {
  private static final Set<String> ALLOWED_MEDIA_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "video/mp4", "video/webm");
  private static final long MAX_FILE_SIZE_BYTES = 50L * 1024L * 1024L;
  private static final String POST_MEDIA_PREFIX = "posts/media/";
  private static final String PROFILE_PICTURE_PREFIX = "users/profile-pictures/";

  private final S3Client s3Client;

  @Value("${garage.bucket-name}")
  private String bucketName;

  @Value("${garage.public-url}")
  private String publicUrl;

  /**
   * Uploads a given file to the S3 file storage and returns a relative filename (or unique object
   * key)
   */
  public String uploadFile(MultipartFile file) throws IOException {
    return uploadFile(file, "");
  }

  public String uploadPostMedia(MultipartFile file) {
    try {
      validatePostMedia(file);
      return uploadFile(file, POST_MEDIA_PREFIX);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload file", e);
    }
  }

  public String uploadProfilePicture(MultipartFile file) {
    try {
      validateProfilePicture(file);
      return uploadFile(file, PROFILE_PICTURE_PREFIX);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload profile picture", e);
    }
  }

  public StoredFile loadFile(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank()) {
      throw new InvalidRequestException("File reference must not be blank");
    }

    try {
      ResponseBytes<GetObjectResponse> objectBytes =
          s3Client.getObjectAsBytes(
              GetObjectRequest.builder().bucket(bucketName).key(mediaRef).build());
      return new StoredFile(
          objectBytes.asByteArray(),
          objectBytes.response().contentType(),
          objectBytes.response().contentLength());
    } catch (S3Exception e) {
      if (e.statusCode() == HttpStatus.NOT_FOUND.value()) {
        throw new ResourceNotFoundException("File not found: " + mediaRef);
      }
      throw e;
    }
  }

  private String uploadFile(MultipartFile file, String prefix) throws IOException {
    String uniqueFileName = prefix + UUID.randomUUID() + resolveExtension(file);

    // build upload request
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(uniqueFileName)
            .contentType(file.getContentType())
            .build();

    // upload file to garage
    s3Client.putObject(
        putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    return uniqueFileName;
  }

  /**
   * Resolves the given relative filename (or unique object key) to a publicly available URL to see
   * the image
   */
  public String resolveFileUrl(String relativeFilename) {
    if (relativeFilename == null || relativeFilename.isBlank()) {
      return null;
    }

    return publicUrl + "/" + bucketName + "/" + relativeFilename;
  }

  private void validatePostMedia(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidRequestException("File must not be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new InvalidRequestException("File must not be larger than 50 MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MEDIA_TYPES.contains(contentType)) {
      throw new InvalidRequestException("Only image and video files are supported");
    }
  }

  private void validateProfilePicture(MultipartFile file) {
    validatePostMedia(file);

    String contentType = file.getContentType();
    if (contentType == null || !contentType.startsWith("image/")) {
      throw new InvalidRequestException("Only image files are supported as profile picture");
    }
  }

  private String resolveExtension(MultipartFile file) {
    String originalFilename = file.getOriginalFilename();
    if (originalFilename == null || !originalFilename.contains(".")) {
      return "";
    }

    String extension =
        originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
    return extension.matches("\\.[a-z0-9]+") ? extension : "";
  }

  public record StoredFile(byte[] content, String contentType, long size) {}
}
