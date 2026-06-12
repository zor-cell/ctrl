package at.ac.tuwien.inso.ctrl.topic;

import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import at.ac.tuwien.inso.ctrl.topic.service.TopicService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/topics")
@RequiredArgsConstructor
public class TopicController {
  private final TopicService topicService;

  @GetMapping
  public List<TopicResponse> getAvailableTopics() {
    return topicService.getAvailableTopics();
  }
}
