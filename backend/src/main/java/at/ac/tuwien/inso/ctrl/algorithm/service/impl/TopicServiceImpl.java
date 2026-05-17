package at.ac.tuwien.inso.ctrl.algorithm.service.impl;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.mapper.TopicMapper;
import at.ac.tuwien.inso.ctrl.algorithm.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.algorithm.service.TopicService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
  private final TopicRepository topicRepository;
  private final TopicMapper topicMapper;

  @Override
  public List<TopicResponse> getAvailableTopics() {
    return topicRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(topicMapper::toTopicResponse)
        .toList();
  }
}
