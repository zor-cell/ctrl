package at.ac.tuwien.inso.ctrl.session;

import at.ac.tuwien.inso.ctrl.session.dto.SessionConfigDto;
import at.ac.tuwien.inso.ctrl.session.entity.SessionConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionMapper {
    SessionConfigDto toConfig(SessionConfig sessionConfig);
}
