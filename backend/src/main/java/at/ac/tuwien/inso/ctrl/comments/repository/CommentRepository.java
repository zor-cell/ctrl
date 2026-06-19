package at.ac.tuwien.inso.ctrl.comments.repository;

import at.ac.tuwien.inso.ctrl.post.entity.Comment;
import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

  @Query(
      """
      SELECT c FROM Comment c
      JOIN FETCH c.user
      LEFT JOIN FETCH c.parentComment
      WHERE c.post.postId = :postId
      """)
  List<Comment> findByPost_PostId(String postId);

  List<Comment> findCommentsByUserAndPost(User user, Post post);

  List<Comment> findAllByUser_Id(UUID userId);
}
