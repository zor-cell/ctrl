package at.ac.tuwien.inso.ctrl.search.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;

public record PostSearchRequest(
    String query, List<String> topicIds, @Min(0) int page, @Min(1) @Max(50) int postsPerPage) {

  public PostSearchRequest {
    if (page < 0) {
      page = 0;
    }
    if (postsPerPage == 0) {
      postsPerPage = 20;
    }
  }
}
