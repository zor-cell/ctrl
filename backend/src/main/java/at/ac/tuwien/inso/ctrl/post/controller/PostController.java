package at.ac.tuwien.inso.ctrl.post.controller;

import at.ac.tuwien.inso.ctrl.post.dto.request.CreatePostRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.PostResponse;
import at.ac.tuwien.inso.ctrl.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
  private final PostService postService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostResponse createPost(
      @Valid @RequestBody CreatePostRequest request,
      @AuthenticationPrincipal UserDetails userDetails) {
    return postService.createPost(request, userDetails.getUsername());
  }
}
