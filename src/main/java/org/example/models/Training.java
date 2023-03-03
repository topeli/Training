package org.example.models;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import javax.persistence.*;
import java.time.LocalDateTime;
@EntityScan("org.example.models")
@Entity
@Table(name = "trainings")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Training {
    private Long group_id;
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long training_id;
    @ManyToOne(fetch = FetchType.EAGER,optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    @JoinColumn(name = "coach_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Student student;
    private Long coach_id;
    private LocalDateTime start_time;
    private LocalDateTime end_time;

    public void setCoach(Coach coach) {
    }
}
