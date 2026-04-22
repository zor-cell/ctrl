package at.ac.tuwien.inso.ctrl.user.entity;

import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers;
  @Column(name = "bio")
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
    return List.of();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
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
