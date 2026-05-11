package at.ac.tuwien.inso.ctrl.feed.repository;

import at.ac.tuwien.inso.ctrl.post.entity.ExternalAuthor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExternalAuthorRepository extends JpaRepository<ExternalAuthor, String> {

  Optional<ExternalAuthor> findByExternalAuthorId(String externalAuthorId);
}
