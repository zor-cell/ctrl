package at.ac.tuwien.inso.ctrl.chat.entity;

import at.ac.tuwien.inso.ctrl.post.entity.Post;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
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
@Table(name = "shared")
public class Shared {
  @Id
  @Column(name = "shared_id", nullable = false, unique = true)
  private UUID sharedId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "post_id",
      referencedColumnName = "post_id",
      nullable = true,
      foreignKey =
          @ForeignKey(
              foreignKeyDefinition =
                  "FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE SET NULL"))
  private Post post;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shared_by_user_id", referencedColumnName = "id", nullable = false)
  private User sharedBy;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;
}
