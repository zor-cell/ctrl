package at.ac.tuwien.inso.ctrl.file;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class FileStorageService {
  private final S3Client s3Client;

  @Value("${garage.bucket-name}")
  private String bucketName;

  @Value("${garage.public-url}")
  private String publicUrl;

  /**
   * Uploads a given file to the S3 file storage and returns a relative filename (or unique object key)
   */
  public String uploadFile(MultipartFile file) throws IOException {
    // generate unique object key
    String originalFilename = file.getOriginalFilename();
    String extension =
        originalFilename != null
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : "";
    String uniqueFileName = UUID.randomUUID().toString() + extension;

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
   * Resolves the given relative filename (or unique object key) to a publicly available URL
   * to see the image
   */
  public String resolveFileUrl(String relativeFilename) {
    if(relativeFilename == null || relativeFilename.isBlank()) {
      return null;
    }

    return publicUrl + "/" + bucketName + "/" + relativeFilename;
  }
}
