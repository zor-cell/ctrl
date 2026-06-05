package at.ac.tuwien.inso.ctrl.post.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "comments")
public class Comment {

  @Id
  @Column(name = "comment_id", nullable = false, unique = true)
  private String commentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "post_id", referencedColumnName = "post_id", nullable = false)
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String content;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "is_removed")
  private Boolean isRemoved = false;

  @Column(nullable = false)
  @Builder.Default
  private boolean edited = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_comment_id", referencedColumnName = "comment_id")
  private Comment parentComment;

  // Needed for cascade-deleting all replies when a parent comment is deleted.
  // Not exposed via API directly — replies are assembled in the service layer.
  @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<Comment> replies = new ArrayList<>();
}
