package at.ac.tuwien.inso.ctrl.post.entity;

import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "posts")
public class Post {
  @Id
  @Column(name = "post_id", nullable = false, unique = true)
  private String postId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
  private User user;

  @OneToOne(optional = true, fetch = FetchType.LAZY)
  @JoinColumn(name = "source_id")
  private Source source;

  private String caption;

  @Column(name = "media_ref")
  private String mediaRef;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic;

  @Builder.Default
  @Column(name = "is_ai_generated")
  private Boolean isAiGenerated = false;

  private Integer upvotes;

  private Integer downvotes;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(name = "embedding")
  @Array(length = 768)
  private float[] embedding;

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<PostLabel> postLabels = new LinkedHashSet<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<PostTopic> postTopics = new LinkedHashSet<>();

  @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<Comment> comments = new LinkedHashSet<>();

  public boolean isAiGenerated() {
    return Boolean.TRUE.equals(isAiGenerated);
  }
}
