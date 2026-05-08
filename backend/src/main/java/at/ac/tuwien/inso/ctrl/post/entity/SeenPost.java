package at.ac.tuwien.inso.ctrl.post.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "seen_posts",
    indexes = {@Index(name = "idx_seen_user_post", columnList = "user_id, post_id")}) //index for quick NOT EXISTS lookups
@IdClass(SeenPostId.class)
public class SeenPost {
  @Id
  @Column(name = "user_id", nullable = false)
  private UUID userId;

  @Id
  @Column(name = "post_id", nullable = false)
  private String postId;

  @CreationTimestamp
  @Column(name = "seen_at", updatable = false)
  private LocalDateTime seenAt;
}
