package at.ac.tuwien.inso.ctrl.post.service.impl;

import at.ac.tuwien.inso.ctrl.post.dto.response.LabelResponse;
import at.ac.tuwien.inso.ctrl.post.mapper.LabelMapper;
import at.ac.tuwien.inso.ctrl.post.repository.LabelRepository;
import at.ac.tuwien.inso.ctrl.post.service.LabelService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LabelServiceImpl implements LabelService {
  private final LabelRepository labelRepository;
  private final LabelMapper labelMapper;

  @Override
  public List<LabelResponse> getAvailableLabels() {
    return labelRepository.findAll(Sort.by(Sort.Direction.ASC, "name")).stream()
        .map(labelMapper::toResponse)
        .toList();
  }
}
