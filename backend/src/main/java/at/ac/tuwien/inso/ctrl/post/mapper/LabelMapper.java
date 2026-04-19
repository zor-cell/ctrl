package at.ac.tuwien.inso.ctrl.post.mapper;

import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.entity.Label;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {
  public LabelResponse toResponse(Label label) {
    return new LabelResponse(label.getLabelId(), label.getName(), label.getEmoji());
  }
}
