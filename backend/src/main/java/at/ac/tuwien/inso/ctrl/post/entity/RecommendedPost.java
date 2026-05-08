package at.ac.tuwien.inso.ctrl.post.entity;

import at.ac.tuwien.inso.ctrl.post.dto.response.PostExplanation;
import lombok.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedPost {
    Post post;
    PostExplanation explanation;
}
