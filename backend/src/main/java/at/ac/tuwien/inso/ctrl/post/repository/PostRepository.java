package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Optional;

import at.ac.tuwien.inso.ctrl.source.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {
  boolean existsByExternalPostId(String externalPostId);

  List<Post> findAllByUserUsernameOrderByCreatedAtDesc(String username);

  Optional<Post> findByPostIdAndUserUsername(String postId, String username);

  Optional<Post> findBySource(Source source);
}
