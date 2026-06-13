package at.ac.tuwien.inso.ctrl.topic;

import at.ac.tuwien.inso.ctrl.topic.dto.TopicCreateRequest;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import at.ac.tuwien.inso.ctrl.topic.service.TopicService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {
  private final TopicService topicService;

  @GetMapping
  public List<TopicResponse> getAvailableTopics() {
    return topicService.getAvailableTopics();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('ADMIN')")
  public TopicResponse createTopic(@Valid @RequestBody TopicCreateRequest request) {
    return topicService.createTopic(request);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public TopicResponse updateTopic(
      @PathVariable String id, @Valid @RequestBody TopicCreateRequest request) {
    return topicService.updateTopic(id, request);
  }
}
