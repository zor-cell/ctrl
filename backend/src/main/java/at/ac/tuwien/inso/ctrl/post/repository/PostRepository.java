package at.ac.tuwien.inso.ctrl.post.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import at.ac.tuwien.inso.ctrl.source.entity.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, String> {

  boolean existsByExternalPostId(String externalPostId);

  List<Post> findAllByUserUsernameOrderByCreatedAtDesc(String username);

  List<Post> findAllByUserUsernameAndIsPublicTrueOrderByCreatedAtDesc(String username);

  Optional<Post> findByPostIdAndUserUsername(String postId, String username);

  boolean existsByMediaRef(String mediaRef);

  @Query("SELECT p FROM Post p JOIN FETCH p.user WHERE p.mediaRef = :mediaRef")
  Optional<Post> findByMediaRefWithUser(@Param("mediaRef") String mediaRef);

  @Query(
      """
      SELECT p FROM Post p
      JOIN FETCH p.user
      WHERE p.mediaRef = :mediaRef
         OR p.mediaRef LIKE CONCAT('%', :mediaRef)
      """)
  List<Post> findAllByMediaRefReferenceWithUser(@Param("mediaRef") String mediaRef);

  @Query("SELECT p FROM Post p ORDER BY p.createdAt DESC")
  Page<Post> findAllPaginated(Pageable pageable);

  @Query(
      """
    SELECT DISTINCT p FROM Post p
    LEFT JOIN FETCH p.user
    LEFT JOIN FETCH p.externalAuthor
    LEFT JOIN FETCH p.postLabels pl LEFT JOIN FETCH pl.label
    LEFT JOIN FETCH p.postTopics pt LEFT JOIN FETCH pt.topic
    LEFT JOIN FETCH p.source s LEFT JOIN FETCH s.singleSources
    LEFT JOIN FETCH p.comments c LEFT JOIN FETCH c.user
    WHERE p.postId IN :postIds
    """)
  List<Post> findAllWithAssociationsByIds(@Param("postIds") List<String> postIds);

  Optional<Post> findBySource(Source source);

  Optional<Post> getPostByPostId(String contentId);
}
