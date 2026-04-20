package at.ac.tuwien.inso.ctrl.source.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SourceVoteId implements Serializable {

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "user_id")
    private String userId;
}