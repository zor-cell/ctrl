package at.ac.tuwien.inso.ctrl.topic.mapper;

import at.ac.tuwien.inso.ctrl.topic.dto.TopicResponse;
import at.ac.tuwien.inso.ctrl.topic.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TopicMapper {
  TopicResponse toTopicResponse(Topic topic);
}
