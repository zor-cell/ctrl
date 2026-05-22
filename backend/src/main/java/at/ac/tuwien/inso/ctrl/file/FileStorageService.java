package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
public class FileStorageService {
  private static final Set<String> ALLOWED_MEDIA_TYPES =
      Set.of(
          "image/jpeg",
          "image/png",
          "image/webp",
          "image/gif",
          "video/mp4",
          "video/webm",
          "video/quicktime",
          "video/x-quicktime");
  private static final Set<String> QUICKTIME_MEDIA_TYPES =
      Set.of("video/quicktime", "video/x-quicktime");
  private static final long MAX_FILE_SIZE_BYTES = 100L * 1024L * 1024L;
  private static final String MP4_MEDIA_TYPE = "video/mp4";
  private static final String POST_MEDIA_PREFIX = "posts/media/";
  private static final String PROFILE_PICTURE_PREFIX = "users/profile-pictures/";

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final S3Presigner internalS3Presigner;
  private final VideoTranscodingService videoTranscodingService;

  @Value("${garage.bucket-name}")
  private String bucketName;

  @Value("${garage.private-bucket-name:${garage.bucket-name}}")
  private String privateBucketName;

  @Value("${garage.public-bucket-name:${garage.bucket-name}}")
  private String publicBucketName;

  @Value("${garage.public-url}")
  private String publicUrl;

  @Value("${garage.presigned-url-duration:PT2M}")
  private Duration presignedUrlDuration;

  public FileStorageService(
      S3Client s3Client,
      @Qualifier("s3Presigner") S3Presigner s3Presigner,
      @Qualifier("internalS3Presigner") S3Presigner internalS3Presigner,
      VideoTranscodingService videoTranscodingService) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
    this.internalS3Presigner = internalS3Presigner;
    this.videoTranscodingService = videoTranscodingService;
  }

  /**
   * Uploads a given file to the S3 file storage and returns a relative filename (or unique object
   * key)
   */
  public String uploadFile(MultipartFile file) throws IOException {
    return uploadFile(file, "").mediaRef();
  }

  public UploadedFile uploadPostMedia(MultipartFile file) {
    try {
      validatePostMedia(file);
      if (isQuickTimeVideo(file)) {
        return transcodeAndUploadQuickTimeVideo(file);
      }
      return uploadFile(file, POST_MEDIA_PREFIX, privateBucketName);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload file", e);
    }
  }

  public String uploadProfilePicture(MultipartFile file) {
    try {
      validateProfilePicture(file);
      return uploadFile(file, PROFILE_PICTURE_PREFIX, publicBucketName).mediaRef();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload profile picture", e);
    }
  }

  public void deleteProfilePicture(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank()) {
      return;
    }

    if (!mediaRef.startsWith(PROFILE_PICTURE_PREFIX)) {
      throw new InvalidRequestException("Invalid profile picture reference");
    }

    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(publicBucketName).key(mediaRef).build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  public void deletePostMedia(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank() || !mediaRef.startsWith(POST_MEDIA_PREFIX)) {
      throw new InvalidRequestException("Invalid post media reference");
    }

    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(privateBucketName).key(mediaRef).build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  public void deletePublicPostMedia(String mediaRef) {
    if (mediaRef == null || mediaRef.isBlank() || !mediaRef.startsWith(POST_MEDIA_PREFIX)) {
      throw new InvalidRequestException("Invalid post media reference");
    }

    DeleteObjectRequest deleteObjectRequest =
        DeleteObjectRequest.builder().bucket(publicBucketName).key(mediaRef).build();

    s3Client.deleteObject(deleteObjectRequest);
  }

  public void movePostMediaToPublicBucket(String mediaRef) {
    if (mediaRef == null
        || mediaRef.isBlank()
        || !mediaRef.startsWith(POST_MEDIA_PREFIX)
        || mediaRef.startsWith("http://")
        || mediaRef.startsWith("https://")
        || privateBucketName.equals(publicBucketName)) {
      return;
    }

    CopyObjectRequest copyObjectRequest =
        CopyObjectRequest.builder()
            .sourceBucket(privateBucketName)
            .sourceKey(mediaRef)
            .destinationBucket(publicBucketName)
            .destinationKey(mediaRef)
            .build();

    s3Client.copyObject(copyObjectRequest);
    deletePostMedia(mediaRef);
  }

  public List<String> listPostMediaRefsOlderThan(Duration minimumAge) {
    return listPostMediaRefsOlderThan(minimumAge, privateBucketName);
  }

  public List<String> listPublicPostMediaRefsOlderThan(Duration minimumAge) {
    return listPostMediaRefsOlderThan(minimumAge, publicBucketName);
  }

  private List<String> listPostMediaRefsOlderThan(Duration minimumAge, String bucketName) {
    Instant cutoff = Instant.now().minus(minimumAge);
    List<String> mediaRefs = new ArrayList<>();
    String continuationToken = null;

    do {
      ListObjectsV2Request request =
          ListObjectsV2Request.builder()
              .bucket(bucketName)
              .prefix(POST_MEDIA_PREFIX)
              .continuationToken(continuationToken)
              .build();
      ListObjectsV2Response response = s3Client.listObjectsV2(request);

      response.contents().stream()
          .filter(object -> object.lastModified() != null)
          .filter(object -> object.lastModified().isBefore(cutoff))
          .map(object -> object.key())
          .forEach(mediaRefs::add);

      continuationToken = response.nextContinuationToken();
    } while (continuationToken != null);

    return mediaRefs;
  }

  public StoredFile readPublicFile(String mediaRef) {
    return readFile(mediaRef, publicBucketName);
  }

  public StoredFile readPrivateFile(String mediaRef) {
    return readFile(mediaRef, privateBucketName);
  }

  public StoredFile readFile(String mediaRef) {
    return readPrivateFile(mediaRef);
  }

  public PresignedFileUrl createPublicPresignedUrl(String mediaRef) {
    return createPresignedUrl(mediaRef, publicBucketName);
  }

  public PresignedFileUrl createPrivatePresignedUrl(String mediaRef) {
    return createPresignedUrl(mediaRef, privateBucketName);
  }

  private PresignedFileUrl createPresignedUrl(String mediaRef, String bucketName) {
    return createPresignedUrl(mediaRef, bucketName, s3Presigner);
  }

  public String resolveInternalPostMediaUrl(String mediaRef) {
    return createPresignedUrl(mediaRef, privateBucketName, internalS3Presigner).mediaUrl();
  }

  private PresignedFileUrl createPresignedUrl(
      String mediaRef, String bucketName, S3Presigner presigner) {
    if (mediaRef == null || mediaRef.isBlank()) {
      throw new InvalidRequestException("Invalid media reference");
    }

    Instant expiresAt = Instant.now().plus(presignedUrlDuration);
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(mediaRef).build();
    PresignedGetObjectRequest presignedRequest =
        presigner.presignGetObject(
            request ->
                request.signatureDuration(presignedUrlDuration).getObjectRequest(getObjectRequest));

    return new PresignedFileUrl(presignedRequest.url().toString(), expiresAt);
  }

  private StoredFile readFile(String mediaRef, String bucketName) {
    if (mediaRef == null || mediaRef.isBlank()) {
      throw new InvalidRequestException("Invalid media reference");
    }

    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketName).key(mediaRef).build();
    ResponseInputStream<GetObjectResponse> objectStream = s3Client.getObject(getObjectRequest);
    GetObjectResponse response = objectStream.response();
    return new StoredFile(objectStream, response.contentType(), response.contentLength());
  }

  private UploadedFile uploadFile(MultipartFile file, String prefix) throws IOException {
    return uploadFile(file, prefix, bucketName);
  }

  private UploadedFile uploadFile(MultipartFile file, String prefix, String bucketName)
      throws IOException {
    String uniqueFileName = prefix + UUID.randomUUID() + resolveExtension(file);
    String contentType = file.getContentType();

    // build upload request
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(uniqueFileName)
            .contentType(contentType)
            .build();

    // upload file to garage
    s3Client.putObject(
        putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

    return new UploadedFile(uniqueFileName, contentType, file.getSize());
  }

  private UploadedFile uploadFile(
      Path filePath, String prefix, String contentType, String extension) throws IOException {
    return uploadFile(filePath, prefix, contentType, extension, bucketName);
  }

  private UploadedFile uploadFile(
      Path filePath, String prefix, String contentType, String extension, String bucketName)
      throws IOException {
    String uniqueFileName = prefix + UUID.randomUUID() + extension;

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(bucketName)
            .key(uniqueFileName)
            .contentType(contentType)
            .build();

    s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));

    return new UploadedFile(uniqueFileName, contentType, Files.size(filePath));
  }

  /**
   * Resolves the given relative filename (or unique object key) to a publicly available URL to see
   * the image
   */
  public String resolveFileUrl(String relativeFilename) {
    if (relativeFilename == null || relativeFilename.isBlank()) {
      return null;
    }

    if (relativeFilename.startsWith("http://") || relativeFilename.startsWith("https://")) {
      return relativeFilename;
    }

    return publicUrl + URLEncoder.encode(relativeFilename, StandardCharsets.UTF_8);
  }

  private void validatePostMedia(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new InvalidRequestException("File must not be empty");
    }

    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new InvalidRequestException("File must not be larger than 100 MB");
    }

    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_MEDIA_TYPES.contains(contentType)) {
      throw new InvalidRequestException("Only image and video files are supported");
    }
  }

  private boolean isQuickTimeVideo(MultipartFile file) {
    return QUICKTIME_MEDIA_TYPES.contains(file.getContentType());
  }

  private UploadedFile transcodeAndUploadQuickTimeVideo(MultipartFile file) throws IOException {
    Path inputFile = Files.createTempFile("post-media-", resolveExtension(file));
    Path outputFile = Files.createTempFile("post-media-", ".mp4");

    try (InputStream inputStream = file.getInputStream()) {
      Files.copy(inputStream, inputFile, StandardCopyOption.REPLACE_EXISTING);
      videoTranscodingService.transcodeToMp4(inputFile, outputFile);
      return uploadFile(outputFile, POST_MEDIA_PREFIX, MP4_MEDIA_TYPE, ".mp4", privateBucketName);
    } finally {
      Files.deleteIfExists(inputFile);
      Files.deleteIfExists(outputFile);
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

  public record UploadedFile(String mediaRef, String contentType, long size) {}

  public record StoredFile(InputStream inputStream, String contentType, long size) {}

  public record PresignedFileUrl(String mediaUrl, Instant expiresAt) {}
}
