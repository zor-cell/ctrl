package at.ac.tuwien.inso.ctrl.file;

import at.ac.tuwien.inso.ctrl.file.dto.FileUploadResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import at.ac.tuwien.inso.ctrl.user.repository.FriendRelationRepository;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
  private static final String POST_MEDIA_PREFIX = "posts/media/";
  private static final String PROFILE_PICTURE_PREFIX = "users/profile-pictures/";

  private final FileStorageService fileStorageService;
  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final FriendRelationRepository friendRelationRepository;

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

  @GetMapping("/media")
  public ResponseEntity<InputStreamResource> getMedia(
      @RequestParam String ref, @AuthenticationPrincipal UserDetails userDetails) {
    if (userDetails == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    assertCanAccess(ref, userDetails.getUsername());
    FileStorageService.StoredFile file = fileStorageService.readFile(ref);

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(file.contentType()))
        .contentLength(file.size())
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline()
                .filename(ref.substring(ref.lastIndexOf('/') + 1))
                .build()
                .toString())
        .body(new InputStreamResource(file.inputStream()));
  }

  private void assertCanAccess(String ref, String username) {
    if (ref == null || ref.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media reference");
    }

    if (ref.startsWith(PROFILE_PICTURE_PREFIX)) {
      return;
    }

    if (!ref.startsWith(POST_MEDIA_PREFIX)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid media reference");
    }

    List<Post> matchingPosts = postRepository.findAllByMediaRefReferenceWithUser(ref);
    if (matchingPosts.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
    }

    User viewer =
        userRepository
            .findByUsername(username)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

    boolean canAccess =
        matchingPosts.stream()
            .anyMatch(
                post ->
                    post.isPublic()
                        || post.getUser().getId().equals(viewer.getId())
                        || friendRelationRepository.existsByUsers(post.getUser(), viewer));

    if (!canAccess) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Media access denied");
    }
  }
}
