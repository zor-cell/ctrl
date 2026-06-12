package at.ac.tuwien.inso.ctrl.topic.service.impl;

import at.ac.tuwien.inso.ctrl.embedding.EmbeddingService;
import at.ac.tuwien.inso.ctrl.exception.InvalidRequestException;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicCreateRequest;
import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import at.ac.tuwien.inso.ctrl.topic.mapper.TopicMapper;
import at.ac.tuwien.inso.ctrl.topic.repository.TopicRepository;
import at.ac.tuwien.inso.ctrl.topic.service.TopicService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TopicServiceImpl implements TopicService {
  private final TopicRepository topicRepository;
  private final TopicMapper topicMapper;
  private final EmbeddingService embeddingService;

  @Override
  public List<TopicResponse> getAvailableTopics() {
    return topicRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(topicMapper::toTopicResponse)
        .toList();
  }

  @Override
  public TopicResponse createTopic(TopicCreateRequest request) {
    if (topicRepository.existsByName(request.name())) {
      throw new InvalidRequestException("Topic already exists");
    }

    String context = request.name() + ": " + request.description();
    float[] embedding = embeddingService.embedText(context);

    Topic topic =
        Topic.builder().name(request.name()).emoji(request.emoji()).embedding(embedding).build();

    Topic savedTopic = topicRepository.save(topic);
    return topicMapper.toTopicResponse(savedTopic);
  }
}
