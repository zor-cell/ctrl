package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
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

  @PostMapping(value = "/post-media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public FileUploadResponse uploadPostMedia(@RequestPart("file") MultipartFile file) {
    FileStorageService.UploadedFile uploadedFile = fileStorageService.uploadPostMedia(file);
    return new FileUploadResponse(
        uploadedFile.mediaRef(),
        fileStorageService.resolveFileUrl(uploadedFile.mediaRef()),
        uploadedFile.contentType(),
        uploadedFile.size());
  }

  @DeleteMapping("/post-media")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deletePostMedia(@RequestParam String ref) {
    fileStorageService.deletePostMedia(ref);
  }
}
