package at.ac.tuwien.inso.ctrl.post.entity;

import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SeenPostId implements Serializable {
    private UUID userId;
    private String postId;
}
