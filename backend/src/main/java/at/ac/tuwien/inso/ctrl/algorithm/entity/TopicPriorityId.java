package at.ac.tuwien.inso.ctrl.algorithm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class TopicPriorityId implements Serializable {
    @Column(name = "username")
    private String username;

    @Column(name = "topic_id")
    private String topicId;
}
