package at.ac.tuwien.inso.ctrl.post.entity;

import jakarta.persistence.*;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "external_authors")
public class ExternalAuthor {

  @Id
  @Column(name = "external_author_id", nullable = false, unique = true)
  private String externalAuthorId; // e.g. Mastodon account.id

  @Column(name = "source", nullable = false)
  private String source; // e.g. "mastodon"

  @Column(name = "display_name", nullable = false)
  private String displayName; // e.g. "tagesschau"

  @Column(name = "avatar_url")
  private String avatarUrl;

  @Column(name = "profile_url")
  private String profileUrl;

  @Column(name="is_suspended", nullable = false)
  private boolean isSuspended = false;
}
