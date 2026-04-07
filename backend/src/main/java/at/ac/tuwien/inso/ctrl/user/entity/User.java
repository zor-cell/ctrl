package at.ac.tuwien.inso.ctrl.user.entity;

import at.ac.tuwien.inso.ctrl.chat.entity.ChatMember;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "birthdate")
    private LocalDate birthdate;

    @OneToMany(mappedBy = "username", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatMember> chatMembers;

    @OneToMany(mappedBy = "follower", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<FollowRelation> followingRelations = new LinkedHashSet<>();

    @OneToMany(mappedBy = "following", cascade = CascadeType.ALL, orphanRemoval = true)
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
