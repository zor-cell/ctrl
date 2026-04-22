package at.ac.tuwien.inso.ctrl.source.service.impl;

import at.ac.tuwien.inso.ctrl.common.vote.VoteManager;
import at.ac.tuwien.inso.ctrl.post.dto.request.VoteRequest;
import at.ac.tuwien.inso.ctrl.post.dto.response.VoteResponse;
import at.ac.tuwien.inso.ctrl.source.dto.request.CreateSourceRequest;
import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import at.ac.tuwien.inso.ctrl.source.mapper.SourceMapper;
import at.ac.tuwien.inso.ctrl.source.repository.SourceRepository;
import at.ac.tuwien.inso.ctrl.source.repository.SourceVoteRepository;
import at.ac.tuwien.inso.ctrl.source.service.SourceService;
import at.ac.tuwien.inso.ctrl.user.repository.UserRepository;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SourceServiceImpl implements SourceService {

  private final SourceRepository sourceRepository;
  private final SourceMapper sourceMapper;
  private final SourceVoteRepository sourceVoteRepository;
  private final UserRepository userRepository;
  private final VoteManager voteManager;


  @Override
  @Transactional
  public Source createSource(CreateSourceRequest createSourceRequest) {
    if (createSourceRequest == null) {
      return null;
    }

    return sourceRepository.save(sourceMapper.toEntity(createSourceRequest));
  }

  @Override
  @Transactional
  public VoteResponse voteSource(String sourceId, VoteRequest request, String username) {
    Source source = fetchSource(sourceId);
    var user = fetchUser(username);

    String newVoteType = request.voteType();
    SourceVoteId voteId = new SourceVoteId(sourceId, user.getId().toString());
    Optional<SourceVote> existingVote = sourceVoteRepository.findById(voteId);

    return voteManager.processVote(
            source,
            existingVote,
            newVoteType,
            /* createVote */ () -> sourceVoteRepository.save(
                    SourceVote.builder().id(voteId).source(source).voteType(newVoteType).build()),
            /* deleteVote */ () -> sourceVoteRepository.delete(existingVote.get()),
            /* updateVote */ () -> {
              existingVote.get().setVoteType(newVoteType);
              sourceVoteRepository.save(existingVote.get());
            },
            /* saveEntity */ () -> sourceRepository.save(source));
  }

  /** Helpers **/

  private Source fetchSource(String sourceId) {
    return sourceRepository.findById(sourceId)
            .orElseThrow(() -> new EntityNotFoundException("Source not found: " + sourceId));
  }

  private User fetchUser(String username) {
    return userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
  }
}