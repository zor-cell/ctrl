package at.ac.tuwien.inso.ctrl.algorithm.mapper;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.springframework.stereotype.Component;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface TopicMapper {
  TopicResponse toTopicResponse(Topic topic);
}
