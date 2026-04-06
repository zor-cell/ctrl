package at.ac.tuwien.inso.ctrl.settings.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
@Table(name = "time_management")
public class TimeManagement {
    @Id
    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "is_day_hardcutted")
    private Boolean isDayHardcutted;

    @Column(name = "duration_per_day")
    private Integer durationPerDay;

    @Column(name = "posts_per_day")
    private Integer postsPerDay;
}
