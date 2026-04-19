package at.ac.tuwien.inso.ctrl.user.mapper;

import at.ac.tuwien.inso.ctrl.user.dto.UserDetailsResponse;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {
  @Mapping(source = "verified", target = "isVerified")
  UserDetailsResponse toUserDetailsResponse(User user);
}
