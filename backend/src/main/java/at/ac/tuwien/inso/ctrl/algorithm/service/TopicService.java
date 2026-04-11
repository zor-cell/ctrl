package at.ac.tuwien.inso.ctrl.algorithm.service;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import java.util.List;

/** Provides available post topics. */
public interface TopicService {
  List<TopicResponse> getAvailableTopics();
}
