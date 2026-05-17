package at.ac.tuwien.inso.ctrl.source.mapper;

import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSingleSourceRequest;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.SingleSource;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SourceMapper {

  public Source toEntity(CreateSourceRequest request) {
    var source =
        Source.builder()
            .sourceId(UUID.randomUUID().toString())
            .upvotes(request.upvotes() != null ? request.upvotes() : 0)
            .downvotes(request.downvotes() != null ? request.downvotes() : 0)
            .createdAt(LocalDateTime.now())
            .singleSources(new ArrayList<>())
            .build();

    if (request.singleSources() == null) {
      return source;
    }

    source.setSingleSources(
        request.singleSources().stream()
            .map(singleSourceRequest -> toEntity(singleSourceRequest, source))
            .toList());

    return source;
  }

  public SingleSource toEntity(CreateSingleSourceRequest request, Source source) {
    return SingleSource.builder()
        .singleSourceId(UUID.randomUUID().toString())
        .journal(request.journal())
        .author(request.author())
        .title(request.title())
        .url(request.url())
        .date(request.date())
        .source(source)
        .build();
  }
}
