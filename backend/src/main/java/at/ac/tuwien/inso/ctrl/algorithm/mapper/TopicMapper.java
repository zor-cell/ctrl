package at.ac.tuwien.inso.ctrl.algorithm.mapper;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import org.springframework.stereotype.Component;

@Component
public class TopicMapper {
  public TopicResponse toResponse(Topic topic) {
    return new TopicResponse(topic.getTopicId(), topic.getName(), topic.getEmoji());
  }
}
