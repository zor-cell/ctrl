package at.ac.tuwien.inso.ctrl.session.mapper;

import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionMapper {
  @Mapping(source = "forcedCutoff", target = "isForcedCutoff")
  SessionConfigDto toConfig(SessionConfig sessionConfig);
}
