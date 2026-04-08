package at.ac.tuwien.inso.ctrl.chat.entity;

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
public class ReactionId implements Serializable {
  @Column(name = "from_username")
  private String fromUsername;

  @Column(name = "message_id")
  private String messageId;
}
