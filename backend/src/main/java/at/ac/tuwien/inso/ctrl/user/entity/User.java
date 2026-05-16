package at.ac.tuwien.inso.ctrl.user.entity;

import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import at.ac.tuwien.inso.ctrl.embedding.config.EmbeddingModelConfig;
import at.ac.tuwien.inso.ctrl.user.dto.Role;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.Array;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(unique = true, nullable = false)
  private String username;

  @Column(nullable = false)
  private String password;

  private String name;

  private String email;

  @Column(name = "is_verified")
  private boolean isVerified;

  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;

  @Column(name = "birthdate")
  private LocalDate birthdate;

  @Builder.Default
  @Column(name = "is_email_verified", nullable = false)
  private boolean emailVerified = false;

  @Column(name="is_suspended", nullable = false)
  private boolean isSuspended = false;

  @Enumerated(EnumType.STRING)
  @Column(name = "role")
  @Builder.Default
  private Role role = Role.USER;

  @JdbcTypeCode(SqlTypes.VECTOR)
  @Column(name = "interaction_preference_embedding")
  @Array(length = EmbeddingModelConfig.MODEL_DIMENSIONS)
  private float[] interactionPreferenceEmbedding;

  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ChatMember> chatMembers;

  @Column(name = "bio", length = 280)
  private String bio;

  @Column(name = "profile_picture_ref")
  private String profilePicture;

  @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<FollowRelation> followedRelations = new LinkedHashSet<>();

  @OneToMany(mappedBy = "followed", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private Set<FollowRelation> followerRelations = new LinkedHashSet<>();

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + resolveRole().name()));
  }

  @PrePersist
  @PreUpdate
  private void applyDefaults() {
    if (role == null) {
      role = Role.USER;
    }
  }

  private Role resolveRole() {
    return role == null ? Role.USER : role;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !isSuspended;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return true;
  }
}
