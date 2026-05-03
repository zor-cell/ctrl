package at.ac.tuwien.inso.ctrl.source.repository;

import at.ac.tuwien.inso.ctrl.source.entity.SourceVote;
import at.ac.tuwien.inso.ctrl.source.entity.SourceVoteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceVoteRepository extends JpaRepository<SourceVote, SourceVoteId> {
  List<SourceVote> findByIdUserIdAndIdSourceIdIn(String userId, List<String> sourceIds);
}
