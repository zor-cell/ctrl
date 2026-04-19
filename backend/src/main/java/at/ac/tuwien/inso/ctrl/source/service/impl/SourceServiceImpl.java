package at.ac.tuwien.inso.ctrl.source.service.impl;

import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.mapper.SourceMapper;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {
  private final SourceRepository sourceRepository;
  private final SourceMapper sourceMapper;

  @Override
  @Transactional
  public Source createSource(CreateSourceRequest createSourceRequest) {
    if (createSourceRequest == null) {
      return null;
    }

    return sourceRepository.save(sourceMapper.toEntity(createSourceRequest));
  }
}
