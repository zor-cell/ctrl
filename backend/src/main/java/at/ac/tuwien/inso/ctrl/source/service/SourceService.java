package at.ac.tuwien.inso.ctrl.source.service;

import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;

public interface SourceService {

  /**
   * Creates a new source from request data when source details are provided.
   *
   * @param createSourceRequest the optional request used to create a new source
   * @return the newly created source, or {@code null} if no source details are provided
   */
  Source createSource(CreateSourceRequest createSourceRequest);
}
