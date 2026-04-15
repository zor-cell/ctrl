package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
  private final FileStorageService fileStorageService;

  @GetMapping("/media")
  public ResponseEntity<byte[]> getMedia(@RequestParam("ref") String mediaRef) {
    FileStorageService.StoredFile storedFile = fileStorageService.loadFile(mediaRef);
    String contentType =
        storedFile.contentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : storedFile.contentType();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .contentLength(storedFile.size())
        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
        .body(storedFile.content());
  }

  @PostMapping(value = "/post-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public FileUploadResponse uploadPostMedia(@RequestPart("file") MultipartFile file) {
    String mediaRef = fileStorageService.uploadPostMedia(file);
    return new FileUploadResponse(
        mediaRef,
        fileStorageService.resolveFileUrl(mediaRef),
        file.getContentType(),
        file.getSize());
  }
}
