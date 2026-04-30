package at.ac.tuwien.inso.ctrl.report.service;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.post.repository.PostRepository;
import at.ac.tuwien.inso.ctrl.report.dto.CreateReportRequest;
import at.ac.tuwien.inso.ctrl.report.entity.ContentTypeEnum;
import at.ac.tuwien.inso.ctrl.report.repository.ReportRepository;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImp implements ReportService {
  private final ReportRepository reportRepository;
  private final PostRepository postRepository;
  private final SourceRepository sourceRepository;
  
  @Override
  public void makeReport(CreateReportRequest createReportRequest) {
    User contentUser = getUserFromContent(createReportRequest);

    
  }
  
  private User getUserFromContent(CreateReportRequest createReportRequest) {
    if (createReportRequest.contentType().equals(ContentTypeEnum.POST)) {
      Post post = postRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Post not found: " + createReportRequest.contentId()));
      
      return post.getUser();

    } else if (createReportRequest.contentType().equals(ContentTypeEnum.SOURCE)) {

      Source source = sourceRepository.findById(createReportRequest.contentId()).orElseThrow(()
          -> new EntityNotFoundException("Source not found: " + createReportRequest.contentId()));
      Post post = postRepository.findBySource(source).orElseThrow(() ->
          new EntityNotFoundException("Post not found for source: " + createReportRequest.contentId()));
      return post.getUser();

    } else {
      throw new IllegalArgumentException("Invalid content type: " + createReportRequest.contentType());
    }
  }
}
