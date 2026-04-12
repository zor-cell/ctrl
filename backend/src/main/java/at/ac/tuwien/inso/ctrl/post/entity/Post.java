package at.ac.tuwien.inso.ctrl.post.entity;

import at.ac.tuwien.inso.ctrl.source.entity.Source;
import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

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
    @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
    private User user;

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id")
    private Source source;

    private String caption;

    @Column(name = "media_ref")
    private String mediaRef;

    private Integer upvotes;

    private Integer downvotes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<PostLabel> postLabels = new LinkedHashSet<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Comment> comments = new LinkedHashSet<>();
}
