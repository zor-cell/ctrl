package at.ac.tuwien.inso.ctrl.post.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class PostTopicId implements Serializable {
  @Column(name = "post_id")
  private String postId;

  @Column(name = "topic_id")
  private String topicId;
}
