package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
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
  private static final Duration TRANSCODING_TIMEOUT = Duration.ofMinutes(3);
  private static final String MP4_MEDIA_TYPE = "video/mp4";
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
    return uploadFile(file, "").mediaRef();
  }

  public UploadedFile uploadPostMedia(MultipartFile file) {
    try {
      validatePostMedia(file);
      if (isQuickTimeVideo(file)) {
        return transcodeAndUploadQuickTimeVideo(file);
      }
      return uploadFile(file, POST_MEDIA_PREFIX);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload file", e);
    }
  }

  public String uploadProfilePicture(MultipartFile file) {
    try {
      validateProfilePicture(file);
      return uploadFile(file, PROFILE_PICTURE_PREFIX).mediaRef();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to upload profile picture", e);
    }
  }

  private UploadedFile uploadFile(MultipartFile file, String prefix) throws IOException {
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

    return publicUrl + "/" + relativeFilename;
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
      // Die hochgeladene Datei wird in die temporäre inputFile kopiert.
      Files.copy(inputStream, inputFile, StandardCopyOption.REPLACE_EXISTING);
      transcodeToMp4(inputFile, outputFile);
      return uploadFile(outputFile, POST_MEDIA_PREFIX, MP4_MEDIA_TYPE, ".mp4");
    } finally {
      Files.deleteIfExists(inputFile);
      Files.deleteIfExists(outputFile);
    }
  }

  private void transcodeToMp4(Path inputFile, Path outputFile) {
    Path ffmpegLogFile = createFfmpegLogFile();
    // Terminal Befehl fürs Umwandeln
    List<String> command =
        List.of(
            "ffmpeg",
            "-y",
            "-hide_banner",
            "-i",
            inputFile.toString(),
            "-map",
            "0:v:0",
            "-map",
            "0:a:0?",
            "-c:v",
            "libx264",
            "-preset",
            "veryfast",
            "-crf",
            "23",
            "-vf",
            "scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
            "-tag:v",
            "avc1",
            "-c:a",
            "aac",
            "-b:a",
            "128k",
            "-movflags",
            "+faststart",
            outputFile.toString());

    // Befehl für FFmpeg wird übergeben
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true); // Normale Ausgabe und Fehlerausgabe zusammenlegen.
    // Schreib die Ausgabe von FFmpeg in diese Logdatei statt Terminal.
    processBuilder.redirectOutput(ffmpegLogFile.toFile());

    try {
      Process process = processBuilder.start();
      boolean completed =
          process.waitFor(
              TRANSCODING_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);

      if (!completed) {
        process.destroyForcibly();
        throw new InvalidRequestException("Video conversion timed out");
      }

      if (process.exitValue() != 0 || !Files.exists(outputFile) || Files.size(outputFile) == 0) {
        log.warn("FFmpeg conversion failed: {}", readFfmpegLog(ffmpegLogFile));
        throw new InvalidRequestException("Video could not be converted to MP4");
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Video conversion is currently unavailable");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InvalidRequestException("Video conversion was interrupted");
    } finally {
      try {
        Files.deleteIfExists(ffmpegLogFile);
      } catch (IOException e) {
        log.debug("Could not delete FFmpeg log file {}", ffmpegLogFile, e);
      }
    }
  }

  private Path createFfmpegLogFile() {
    try {
      // In diese Datei schreibt FFmpeg später, was beim Video-Umwandeln passiert ist.
      return Files.createTempFile("post-media-ffmpeg-", ".log");
    } catch (IOException e) {
      throw new InvalidRequestException("Video conversion is currently unavailable");
    }
  }

  private String readFfmpegLog(Path ffmpegLogFile) {
    try {
      String output = Files.readString(ffmpegLogFile, StandardCharsets.UTF_8).trim();
      if (output.length() > 4000) {
        return output.substring(output.length() - 4000);
      }
      return output;
    } catch (IOException e) {
      return "FFmpeg output could not be read";
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
}
