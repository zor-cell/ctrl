package at.ac.tuwien.inso.ctrl.algorithm.mapper;

import at.ac.tuwien.inso.ctrl.algorithm.dto.response.AlgorithmConfigResponse;
import at.ac.tuwien.inso.ctrl.algorithm.entity.AlgorithmConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AlgorithmMapper {
    AlgorithmConfigResponse toConfig(AlgorithmConfig algorithmConfig);
}
