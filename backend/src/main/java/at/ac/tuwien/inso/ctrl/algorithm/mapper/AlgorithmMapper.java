package at.ac.tuwien.inso.ctrl.algorithm.mapper;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.dto.response.TopicResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import at.ac.tuwien.inso.ctrl.algorithm.entity.Topic;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlgorithmMapper {
    AlgorithmConfigResponse toConfig(AlgorithmConfig algorithmConfig);

    TopicResponse toTopicResponse(Topic topic);
}
