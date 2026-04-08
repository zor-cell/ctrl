package at.ac.tuwien.inso.ctrl.algorithm.entity;

import at.ac.tuwien.inso.ctrl.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "topic_priorities")
public class TopicPriority {

  @EmbeddedId private TopicPriorityId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("username")
  @JoinColumn(name = "username", referencedColumnName = "username", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("topicId")
  @JoinColumn(name = "topic_id", referencedColumnName = "topic_id", nullable = false)
  private Topic topic;

  @Column(nullable = false)
  private Integer rank;
}
