package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.PostVote;
import at.ac.tuwien.inso.ctrl.post.entity.PostVoteId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostVoteRepository extends JpaRepository<PostVote, PostVoteId> {

  Optional<PostVote> findById(PostVoteId id);

  List<PostVote> findByIdUserIdAndIdPostIdIn(String userId, List<String> postIds);
}
