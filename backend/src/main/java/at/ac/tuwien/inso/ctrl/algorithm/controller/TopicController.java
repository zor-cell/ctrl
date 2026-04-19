package at.ac.tuwien.inso.ctrl.algorithm.controller;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.service.TopicService;
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
