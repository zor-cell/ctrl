package at.ac.tuwien.inso.ctrl.topic.service;

import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import java.util.List;

/** Provides available post topics. */
public interface TopicService {
  List<TopicResponse> getAvailableTopics();
}
