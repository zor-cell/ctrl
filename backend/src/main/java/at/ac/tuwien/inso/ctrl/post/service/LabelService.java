package at.ac.tuwien.inso.ctrl.post.service;

import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import java.util.List;

public interface LabelService {

  /**
   * Returns all available labels that can be assigned to posts.
   *
   * @return the available labels ordered for presentation
   */
  List<LabelResponse> getAvailableLabels();
}
